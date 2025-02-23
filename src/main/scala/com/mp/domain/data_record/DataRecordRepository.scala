package com.mp.domain.data_record

import akka.stream.scaladsl.Source
import com.mp.domain.data_record.report.DataRecordReportCriteria

import scala.concurrent.Future

trait DataRecordRepository {
  def save(dataRecord: DataRecord): Future[Unit]
  def getNextRecordToProcess(criteria: DataRecordProcessCriteria): Future[Option[DataRecord]]
  def getSourceForReport(criteria: DataRecordReportCriteria): Source[DataRecord, Unit]
}
