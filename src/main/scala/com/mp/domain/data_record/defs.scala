package com.mp.domain.data_record

import com.mp.domain.data_record.process_configuration.DataRecordProcessConfig
import com.mp.domain.shared.{Amount, Name, PhoneNumber}

import java.time.Instant

case class DataRecordId(value: String)
case class DataRecord(
    id: DataRecordId,
    name: Name,
    phoneNumber: PhoneNumber,
    amount: Amount,
    createTime: Instant,
    processTime: Option[Instant]
) {
  def setProcessed(time: Instant): DataRecord = copy(processTime = Some(time))
}

sealed trait DataRecordCommand
object DataRecordCommand {
  case class Create(
      name: Name,
      phoneNumber: PhoneNumber,
      amount: Amount,
  ) extends DataRecordCommand
}

case class DataRecordProcessCriteria(
    priorityByAmountConfig: Set[DataRecordProcessConfig.PriorityByAmountConfig],
    unAllowedProcessEarliestTimeWithSamePhoneNumber: Instant
)
