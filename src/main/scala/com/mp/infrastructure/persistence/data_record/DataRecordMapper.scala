package com.mp.infrastructure.persistence.data_record

import com.mp.domain.data_record.{DataRecord, DataRecordId}
import com.mp.domain.shared.{Amount, Name, PhoneNumber}
import com.mp.infrastructure.persistence.data_record.DataRecordMongoDocs.DataRecordDocument

object DataRecordMapper {
  def toDocument(dataRecord: DataRecord): DataRecordDocument = {
    DataRecordDocument(
      _id = dataRecord.id.value,
      name = dataRecord.name.value,
      phoneNumber = dataRecord.phoneNumber.value,
      amount = dataRecord.amount.value,
      createTime = dataRecord.createTime,
      processTime = dataRecord.processTime
    )
  }

  def toDomain(document: DataRecordDocument): DataRecord = {
    DataRecord(
      id = DataRecordId(document._id),
      name = Name(document.name),
      phoneNumber = PhoneNumber(document.phoneNumber),
      amount = Amount(document.amount),
      createTime = document.createTime,
      processTime = document.processTime
    )
  }
}
