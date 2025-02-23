package com.mp.infrastructure.rest.data_record

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import com.mp.domain.data_record.report.{DataRecordReportCriteria, DataRecordReportFactory}
import com.mp.domain.data_record.{DataRecordCommand, DataRecordService}
import com.mp.domain.shared.{Amount, Name, PhoneNumber}
import com.mp.infrastructure.configuration.http.HttpEndpoint

import java.time.Clock
import scala.language.postfixOps

class DataRecordEndpoint(implicit
    clock: Clock,
    dataRecordService: DataRecordService,
    dataRecordReportFactory: DataRecordReportFactory
) extends HttpEndpoint {
  import DataRecordApi._

  override def route: Route = concat(
    createDataRecordRoute,
    processDataRecordRoute,
    getDataRecordReportRoute
  )

  private val createDataRecordRoute =
    (path("api" / "data-records") & post & entity(as[CreateDataRecordRequest])) { request =>
      onSuccess(
        dataRecordService.createRecord(
          command = DataRecordCommand.Create(
            name = Name(request.name),
            phoneNumber = PhoneNumber(request.phoneNumber),
            amount = Amount(request.amount)
          )
        )
      ) { id =>
        complete(CreateDataRecordResponse(id.value))
      }
    }

  private val processDataRecordRoute =
    (path("api" / "data-records")
      & get) {
      onSuccess(dataRecordService.processRecord()) {
        case None                  => complete(StatusCodes.NoContent)
        case Some(processedRecord) => complete(DataRecordApiMapper.toApi(processedRecord))
      }
    }

  private val getDataRecordReportRoute =
    (path("api" / "data-records" / "report")
      & get
      & parameters("onlyProcessedRecords".as[Boolean] ?)) { onlyProcessedRecords =>
      onSuccess(
        dataRecordReportFactory.generateRecordsReport(DataRecordReportCriteria(onlyProcessedRecords.getOrElse(false)))
      ) { report =>
        complete(DataRecordApiMapper.toApi(report))
      }
    }

}
