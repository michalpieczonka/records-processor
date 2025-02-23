package com.mp.infrastructure.configuration.http

import io.circe.Encoder
import io.circe.generic.semiauto._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import com.mp.domain.shared.exception.DomainException

sealed trait ErrorResponse

object ErrorResponse {
  case class BadRequest(
      code: String,
      message: String
  ) extends ErrorResponse

  object BadRequest {
    def fromDomainException(ex: DomainException): BadRequest = {
      val code    = ex.errors.headOption.map(_.trim.toUpperCase).getOrElse("UNKNOWN_ERROR")
      val message = ex.details.orElse(Option(ex.msg).filter(_.nonEmpty)).getOrElse(code)

      BadRequest(code, message)
    }

    implicit val badRequestEncoder: Encoder[BadRequest]               = deriveEncoder[BadRequest]
    implicit val badRequestMarshaller: ToEntityMarshaller[BadRequest] = marshaller[BadRequest]
  }
}
