package com.mp.infrastructure.rest.data_record

import java.time.{Instant, LocalDate}

object DataRecordApi {
  case class CreateDataRecordRequest(
      name: String,
      phoneNumber: String,
      amount: BigDecimal
  )

  case class CreateDataRecordResponse(
      dataRecordId: String
  )

  sealed trait GetDataRecordResponse
  object GetDataRecordResponse {
    case class ProcessedRecord(
        dataRecordId: String,
        name: String,
        phoneNumber: String,
        amount: BigDecimal,
        createTime: Instant
    ) extends GetDataRecordResponse
  }

  case class GetDataRecordReport(
      entries: Seq[GetDataRecordReport.Entry]
  )
  object GetDataRecordReport {
    case class Entry(
        phoneNumber: String,
        records: Seq[RecordData]
    )

    case class RecordData(
        name: String,
        content: Content
    )

    case class Content(
        amountsSum: BigDecimal,
        newestRecordCreateDate: LocalDate
    )
  }
}
