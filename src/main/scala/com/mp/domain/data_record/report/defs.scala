package com.mp.domain.data_record.report

import java.time.Instant

case class DataRecordReport(
    entries: List[DataRecordReport.Entry]
)
object DataRecordReport {
  case class Entry(
      phoneNumber: String,
      records: List[Entry.Record]
  )
  object Entry {
    case class Record(
        name: String,
        recordsAmountSum: BigDecimal,
        recordsNewestCreateTime: Instant
    )
  }
}

case class DataRecordReportCriteria(
    onlyProcessedRecords: Boolean
)
