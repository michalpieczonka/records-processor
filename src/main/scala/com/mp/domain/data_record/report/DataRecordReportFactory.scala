package com.mp.domain.data_record.report

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink}
import com.mp.domain.data_record.{DataRecord, DataRecordRepository}

import scala.concurrent.Future

class DataRecordReportFactory(implicit
    system: ActorSystem,
    dataRecordRepository: DataRecordRepository
) {
  def generateRecordsReport(criteria: DataRecordReportCriteria): Future[DataRecordReport] = {
    dataRecordRepository
      .getSourceForReport(criteria)
      .via(
        Flow[DataRecord]
          .fold(Map.empty[String, List[DataRecord]]) { (acc, record) =>
            acc.updated(record.phoneNumber.value, acc.getOrElse(record.phoneNumber.value, List()) :+ record)
          }
      )
      .map { groupedRecords =>
        val entries = groupedRecords.map { case (phoneNumber, records) =>
          val groupedByName = records
            .groupBy(_.name.value)
            .values
            .map { recordsByName =>
              val totalAmount      = recordsByName.map(_.amount.value).sum
              val newestCreateTime = recordsByName.map(_.createTime).max
              DataRecordReport.Entry.Record(
                name = recordsByName.head.name.value,
                recordsAmountSum = totalAmount,
                recordsNewestCreateTime = newestCreateTime
              )
            }
            .toList
          DataRecordReport.Entry(
            phoneNumber = phoneNumber,
            records = groupedByName
          )
        }.toList
        DataRecordReport(entries)
      }
      .runWith(Sink.head)
  }
}
