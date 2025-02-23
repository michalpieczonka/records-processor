package com.mp.infrastructure.persistence.data_record.process_configuration

import com.mp.domain.data_record.process_configuration.DataRecordProcessConfig
import com.mp.infrastructure.persistence.data_record.process_configuration.DataRecordProcessConfigMongoDocs.DataRecordProcessConfigDocument

object DataRecordProcessConfigMapper {
  def toDocument(config: DataRecordProcessConfig): DataRecordProcessConfigDocument = {
    DataRecordProcessConfigDocument(
      DataRecordProcessConfigMongoDocs.dataRecordProcessConfigId,
      config.prioritiesByAmount.map { priorityByAmount =>
        DataRecordProcessConfigMongoDocs.PriorityByAmountConfigDocument(
          amountRangeFrom = priorityByAmount.amountRange.from,
          amountRangeTo = priorityByAmount.amountRange.to,
          priority = priorityByAmount.priority.value
        )
      }.toSeq
    )
  }

  def toDomain(document: DataRecordProcessConfigDocument): DataRecordProcessConfig = {
    DataRecordProcessConfig(
      prioritiesByAmount = document.prioritiesByAmount.map { priorityByAmountDocument =>
        DataRecordProcessConfig.PriorityByAmountConfig(
          amountRange = DataRecordProcessConfig.AmountRange(
            from = priorityByAmountDocument.amountRangeFrom,
            to = priorityByAmountDocument.amountRangeTo
          ),
          priority = DataRecordProcessConfig.Priority(priorityByAmountDocument.priority)
        )
      }.toSet
    )
  }
}
