package com.mp.infrastructure.rest.process_configuration

import com.mp.domain.data_record.process_configuration.DataRecordProcessConfig
import com.mp.domain.data_record.process_configuration.DataRecordProcessConfig.PriorityByAmountConfig
import com.mp.infrastructure.rest.data_record.process_configuration.{DataRecordProcessConfigApi, DataRecordProcessConfigApiMapper}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class DataRecordProcessConfigurationApiMapperTest extends AnyFunSuite with Matchers {

  test("toDomain should correctly map UpdateProcessConfigRequest to DataRecordProcessConfig") {
    val request = DataRecordProcessConfigApi.UpdateProcessConfigRequest(
      prioritiesByAmount = Seq(
        DataRecordProcessConfigApi.PriorityByAmountConfig(from = BigDecimal(0), to = Some(BigDecimal(500)), priority = 1),
        DataRecordProcessConfigApi.PriorityByAmountConfig(from = BigDecimal(501), to = None, priority = 2)
      )
    )

    val expectedDomain = DataRecordProcessConfig(
      prioritiesByAmount = Set(
        PriorityByAmountConfig(
          amountRange = DataRecordProcessConfig.AmountRange(from = BigDecimal(0), to = Some(BigDecimal(500))),
          priority = DataRecordProcessConfig.Priority(1)
        ),
        PriorityByAmountConfig(
          amountRange = DataRecordProcessConfig.AmountRange(from = BigDecimal(501), to = None),
          priority = DataRecordProcessConfig.Priority(2)
        )
      )
    )

    val result = DataRecordProcessConfigApiMapper.toDomain(request)
    result shouldEqual expectedDomain
  }

  test("toApi should correctly map DataRecordProcessConfig to GetProcessConfigResponse") {
    val domain = DataRecordProcessConfig(
      prioritiesByAmount = Set(
        PriorityByAmountConfig(
          amountRange = DataRecordProcessConfig.AmountRange(from =  BigDecimal(0), to = Some(BigDecimal(500))),
          priority = DataRecordProcessConfig.Priority(1)
        ),
        PriorityByAmountConfig(
          amountRange = DataRecordProcessConfig.AmountRange(from = BigDecimal(501), to = None),
          priority = DataRecordProcessConfig.Priority(2)
        )
      )
    )

    val expectedResponse = DataRecordProcessConfigApi.GetProcessConfigResponse(
      prioritiesByAmount = Seq(
        DataRecordProcessConfigApi.PriorityByAmountConfig(from = BigDecimal(0), to = Some(BigDecimal(500)), priority = 1),
        DataRecordProcessConfigApi.PriorityByAmountConfig(from = BigDecimal(501), to = None, priority = 2)
      )
    )

    val result = DataRecordProcessConfigApiMapper.toApi(domain)
    result.prioritiesByAmount should contain theSameElementsAs expectedResponse.prioritiesByAmount
  }
}