package com.mp.infrastructure.rest.data_record

import com.mp.domain.data_record.DataRecord
import com.mp.domain.data_record.report.DataRecordReport

import java.time.Clock

object DataRecordApiMapper {
  def toApi(record: DataRecord): DataRecordApi.GetDataRecordResponse.ProcessedRecord = {
    DataRecordApi.GetDataRecordResponse.ProcessedRecord(
      dataRecordId = record.id.value,
      name = record.name.value,
      phoneNumber = record.phoneNumber.value,
      amount = record.amount.value,
      createTime = record.createTime
    )
  }

  def toApi(report: DataRecordReport)(implicit clock: Clock): DataRecordApi.GetDataRecordReport = {
    DataRecordApi.GetDataRecordReport(
      entries = report.entries.map { entry =>
        DataRecordApi.GetDataRecordReport.Entry(
          phoneNumber = entry.phoneNumber,
          records = entry.records.map { record =>
            DataRecordApi.GetDataRecordReport.RecordData(
              name = record.name,
              content = DataRecordApi.GetDataRecordReport.Content(
                amountsSum = record.recordsAmountSum,
                newestRecordCreateDate = record.recordsNewestCreateTime.atZone(clock.getZone).toLocalDate
              )
            )
          }
        )
      }
    )
  }
}
