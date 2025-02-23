package com.mp.infrastructure.configuration.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.HttpMethods.{DELETE, GET, OPTIONS, PATCH, POST, PUT}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.server.{Directive0, Directives, ExceptionHandler, RejectionHandler, Route, RouteResult}
import akka.http.scaladsl.settings.RoutingSettings
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import com.mp.domain.shared.exception.DomainException
import com.typesafe.scalalogging.StrictLogging
import com.typesafe.config.{Config => RawConfig}

import java.util.concurrent.TimeUnit
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

class HttpServer(
    config: HttpServer.Config,
    endpoints: Seq[HttpEndpoint] = Seq.empty,
    corsSettings: CorsSettings = HttpServer.corsSettingsDefault,
    exceptionHandler: ExceptionHandler = HttpServer.exceptionHandlerDefault,
    rejectionHandler: RejectionHandler = HttpServer.rejectionHandlerDefault
)(implicit
    system: ActorSystem,
    ec: ExecutionContext
) extends Directives
    with StrictLogging {
  private val segmentStripRegex = """\{.*?\}""".r

  private var server: Option[ServerBinding] = None
  private val timeoutResponse = HttpResponse(StatusCodes.NetworkReadTimeout, entity = "Unable to serve response within time limit.")

  def start(): Unit = {
    startF().onComplete {
      case Failure(exception) => throw new RuntimeException("HTTP server start error", exception)
      case Success(_) =>
        logger.info(s"HTTP server started, interface=${config.interface} port=${config.port}")
    }
  }

  def startF(): Future[Unit] =
    Http()
      .newServerAt(config.interface, config.port)
      .bind(route())
      .map(s => server = Some(s))

  def stop(): Unit =
    server.foreach(binding =>
      binding
        .terminate(FiniteDuration(10, TimeUnit.SECONDS))
        .onComplete {
          case Failure(exception) => throw new RuntimeException("HTTP server stop error", exception)
          case Success(_)         => logger.info("HTTP server stopped successfully")
        }
    )

  def localPort: Option[Int] =
    server.map(_.localAddress.getPort)

  private def route(): Route = {
    logRequestResponse
      .and(cors(corsSettings))
      .and(handleRejections(rejectionHandler))
      .and(handleExceptions(exceptionHandler.seal(RoutingSettings(system)))) {
        endpoints.map(_.route).reduce(_ ~ _)
      }
  }

  private def logRequestResponse: Directive0 = {
    def aroundRequest(onRequest: HttpRequest => RouteResult => Unit): Directive0 = {
      extractRequestContext.flatMap { ctx =>
        val onDone = onRequest(ctx.request)
        mapInnerRoute { inner =>
          withRequestTimeoutResponse { _ =>
            onDone(RouteResult.Complete(timeoutResponse))
            timeoutResponse
          } {
            inner.andThen { resultFuture =>
              resultFuture.map {
                case c @ RouteResult.Complete(response) =>
                  Complete(response.mapEntity { entity =>
                    if (entity.isKnownEmpty()) {
                      onDone(c)
                      entity
                    } else {
                      entity.transformDataBytes(Flow[ByteString].watchTermination() { case (m, f) =>
                        f.map(_ => c).map(onDone)
                        m
                      })
                    }
                  })
                case other =>
                  onDone(other)
                  other
              }
            }
          }
        }
      }
    }
    def log(req: HttpRequest): RouteResult => Unit = {
      val start = System.currentTimeMillis()

      {
        case RouteResult.Complete(res) =>
          val msgBuilder = new mutable.StringBuilder()
            .append("HTTP request completed:")
            .append(s" method=${req.method.value}")
            .append(s" uri=${req.uri.path.toString}")
            .append(s" status=${res.status.intValue}")
            .append(s" time=${System.currentTimeMillis() - start}ms")
            .append(s" name=${segmentStripRegex.replaceAllIn(req.uri.path.toString, "")}")

          (if (res.status.intValue() >= 400) {
              for {
                reqBody <- extractRequestBody(req)
                resBody <- extractResponseBody(res)

                _ = msgBuilder.append(s" request=$reqBody response=$resBody")
              } yield msgBuilder.result()
            } else {
              Future.successful(msgBuilder.result())
            }
          ).onComplete {
            case Failure(exception) =>
              logger.error(
                s"HTTP request completed but detailed log failed:" +
                  s" method=${req.method.value}" +
                  s" uri=${req.uri.path}" +
                  s" status=${res.status.intValue}",
                exception
              )
            case Success(msg) =>
              if (res.status.intValue() >= 500) logger.error(msg)
              else logger.info(msg)
          }

        case RouteResult.Rejected(rejections) =>
          val rejectionsString = rejections.mkString(", ").stripMargin.replace("\n", " ")
          logger.warn(
            s"HTTP request rejected: " +
              s" method=${req.method.value}" +
              s" uri=${req.uri.path.toString}" +
              s" rejections=$rejectionsString" +
              s" time=${System.currentTimeMillis() - start}ms"
          )
      }
    }
    aroundRequest(log)
  }

  private def extractRequestBody(req: HttpRequest): Future[String] = extractHttpBody(req.entity)

  private def extractResponseBody(res: HttpResponse): Future[String] = extractHttpBody(res.entity)

  private def extractHttpBody[ENTITY <: HttpEntity](entity: ENTITY): Future[String] = {
    if (entity.httpEntity.contentType == ContentTypes.`application/json`) {
      (entity match {
        case entity: HttpEntity.Strict => Future.successful(entity)
        case entity                    => entity.toStrict(30.seconds)
      }).map(_.data.utf8String)
    } else {
      Future.successful(entity.contentType.value)
    }
  }

}
object HttpServer extends Directives with StrictLogging {
  case class Config(
      port: Int = 8080,
      interface: String = "0.0.0.0",
  )

  object Config {
    def from(rawConfig: RawConfig, basePath: String): Config = {
      val config = rawConfig.getConfig(basePath)
      Config(
        port = config.getInt("port"),
        interface =
          if (config.hasPath("interface")) config.getString("interface")
          else "0.0.0.0"
      )
    }
  }

  val corsSettingsDefault: CorsSettings =
    CorsSettings.defaultSettings
      .withAllowedMethods(Seq(GET, POST, PUT, PATCH, DELETE, OPTIONS))

  val exceptionHandlerDefault: ExceptionHandler = ExceptionHandler {
    case ex: DomainException =>
      complete(StatusCodes.BadRequest, ErrorResponse.BadRequest.fromDomainException(ex))

    case NonFatal(err) =>
      (extractMethod & extractUri) { (method, uri) =>
        logger.error(s"HTTP request exception: method=${method.value} uri=${uri.path} status=500", err)
        complete(StatusCodes.InternalServerError)
      }
  }

  val rejectionHandlerDefault: RejectionHandler = RejectionHandler.default
}
