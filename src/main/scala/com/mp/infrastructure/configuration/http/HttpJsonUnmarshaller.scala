package com.mp.infrastructure.configuration.http

import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.stream.Materializer
import akka.util.ByteString
import de.heikoseeberger.akkahttpcirce.BaseCirceSupport
import io.circe.Decoder
import io.circe.generic.AutoDerivation

import scala.concurrent.{ExecutionContext, Future}

trait HttpJsonUnmarshaller extends BaseCirceSupport with AutoDerivation { this: BaseCirceSupport =>
  override implicit final def fromByteStringUnmarshaller[A: Decoder]: Unmarshaller[ByteString, A] = {
    new Unmarshaller[ByteString, A] {
      override def apply(value: ByteString)(implicit ec: ExecutionContext, materializer: Materializer): Future[A] = {
        byteStringJsonUnmarshaller
          .map(Decoder[A].decodeJson)
          .map(_.fold(throw _, identity))(
            ByteString.fromString("\"") ++ value ++ ByteString.fromString("\"")
          )
      }
    }
  }

  override implicit final def unmarshaller[A: Decoder]: FromEntityUnmarshaller[A] =
    jsonUnmarshaller
      .map(Decoder[A].decodeJson)
      .map(_.fold(throw _, identity))
}

