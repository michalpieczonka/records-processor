package com.mp.infrastructure.configuration.http

import akka.http.scaladsl.marshalling.PredefinedToEntityMarshallers
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.unmarshalling.PredefinedFromStringUnmarshallers
import com.typesafe.scalalogging.StrictLogging

trait HttpEndpoint
    extends Directives
    with PredefinedFromStringUnmarshallers
    with PredefinedToEntityMarshallers
    with HttpJsonUnmarshaller
    with StrictLogging {

  def route: Route

}
