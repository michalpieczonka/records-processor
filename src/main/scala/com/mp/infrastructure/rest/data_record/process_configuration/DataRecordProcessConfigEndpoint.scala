package com.mp.infrastructure.rest.data_record.process_configuration

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import com.mp.domain.data_record.process_configuration.DataRecordProcessConfigService
import com.mp.infrastructure.configuration.http.{ErrorResponse, HttpEndpoint}

class DataRecordProcessConfigEndpoint(implicit
    dataRecordProcessConfigService: DataRecordProcessConfigService
) extends HttpEndpoint {
  import DataRecordProcessConfigApi._
  import DataRecordProcessConfigApiMapper._

  override def route: Route = concat(
    updateDataRecordProcessConfigRoute,
    getDataRecordProcessConfigRoute
  )

  private val getDataRecordProcessConfigRoute =
    (path("api" / "data-records" / "process-configuration")
      & get) {
      onSuccess(dataRecordProcessConfigService.get()) { config =>
        complete(toApi(config))
      }
    }

  private val updateDataRecordProcessConfigRoute =
    (path("api" / "data-records" / "process-configuration")
      & put
      & entity(as[UpdateProcessConfigRequest])) { request =>
      val validationErrors = request.validateEntries()
      if (validationErrors.nonEmpty) {
        complete(
          StatusCodes.BadRequest -> ErrorResponse.BadRequest(
            code = "INVALID_REQUEST",
            message = validationErrors.get
          )
        )
      } else {
        val dataRecordProcessConfig = toDomain(request)
        onSuccess(dataRecordProcessConfigService.save(dataRecordProcessConfig))(complete(StatusCodes.NoContent))
      }
    }
}
