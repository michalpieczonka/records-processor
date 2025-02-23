package com.mp.infrastructure.configuration.mongo

import akka.actor.{ActorSystem, Scheduler}
import com.typesafe.config.{Config => RawConfig}
import com.typesafe.scalalogging.StrictLogging
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.{AsyncDriver, DB, MongoConnection}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

class MongoClient(config: MongoClient.Config)(implicit
    ec: ExecutionContext,
    system: ActorSystem
) extends StrictLogging {
  private val driver: AsyncDriver = new AsyncDriver()
  private val parsedUri: MongoConnection.ParsedURI =
    Await.result(MongoConnection.fromString(config.uri), 30.seconds)

  val connection: Future[MongoConnection] = connect()

  private def connect(): Future[MongoConnection] = {
    implicit val scheduler: Scheduler = system.scheduler
    akka.pattern
      .retry(
        () =>
          driver
            .connect(
              nodes = parsedUri.hosts.map { case (host, port) => s"$host:$port" }.toSeq,
            ),
        attempts = 10,
        delay = 10.seconds
      )
      .map { conn =>
        logger.info("Mongo connection success")
        conn
      }
      .recoverWith { case ex: Throwable =>
        logger.error("Mongo connection failed, retrying after 10s", ex)
        akka.pattern.after(10.seconds, scheduler)(connect())
      }
  }

  def db: Future[DB] = connection.flatMap(_.database(config.dbName))

  def collection(collectionsName: String): Future[BSONCollection] =
    db.map(_.collection(collectionsName))
}

object MongoClient {
  case class Config private (uri: String, dbName: String)

  object Config {
    def apply(uri: String, dbName: String): Config = {
      new Config(
        uri =
          if (uri.startsWith("mongodb://")) uri
          else s"mongodb://$uri",
        dbName = dbName
      )
    }

    def apply(host: String, port: Int, dbName: String): Config =
      Config(s"$host:$port", dbName)

    def from(rawConfig: RawConfig, basePath: String): Config = {
      val config = rawConfig.getConfig(basePath)
      if (config.hasPath("uri"))
        Config(
          uri = config.getString("uri"),
          dbName = config.getString("db-name")
        )
      else
        throw new IllegalArgumentException("MongoClient configuration missing uri")
    }
  }

  sealed trait MongoException
  case class MongoWriteException(writeResult: WriteResult)
      extends RuntimeException(writeResult.writeErrors.map(err => s"${err.code}: ${err.errmsg}").mkString(", "))
      with MongoException
}
