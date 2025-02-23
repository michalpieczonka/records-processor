package com.mp.infrastructure.rest.data_record.process_configuration

import com.mp.domain.data_record.process_configuration.DataRecordProcessConfig
import com.mp.domain.data_record.process_configuration.DataRecordProcessConfig.PriorityByAmountConfig

object DataRecordProcessConfigApiMapper {
  def toDomain(request: DataRecordProcessConfigApi.UpdateProcessConfigRequest): DataRecordProcessConfig = {
    DataRecordProcessConfig(
      prioritiesByAmount = request.prioritiesByAmount.map { entry =>
        PriorityByAmountConfig(
          amountRange = DataRecordProcessConfig.AmountRange(
            from = entry.from,
            to = entry.to
          ),
          priority = DataRecordProcessConfig.Priority(entry.priority)
        )
      }.toSet
    )
  }

  def toApi(response: DataRecordProcessConfig): DataRecordProcessConfigApi.GetProcessConfigResponse = {
    DataRecordProcessConfigApi.GetProcessConfigResponse(
      prioritiesByAmount = response.prioritiesByAmount.map { entry =>
        DataRecordProcessConfigApi.PriorityByAmountConfig(
          from = entry.amountRange.from,
          to = entry.amountRange.to,
          priority = entry.priority.value
        )
      }.toSeq
    )
  }
}
