package com.mp.infrastructure.persistence.data_record.process_configuration

import com.mp.domain.data_record.process_configuration.DataRecordProcessConfig
import com.mp.infrastructure.persistence.data_record.process_configuration.DataRecordProcessConfigMongoDocs.DataRecordProcessConfigDocument
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DataRecordProcessConfigMapperTest extends AnyFlatSpec with Matchers {

  "toDocument" should "convert DataRecordProcessConfig to DataRecordProcessConfigDocument" in {
    val config = DataRecordProcessConfig(
      prioritiesByAmount = Set(
        DataRecordProcessConfig.PriorityByAmountConfig(
          amountRange = DataRecordProcessConfig.AmountRange(from = BigDecimal(0), to = Some(BigDecimal(500))),
          priority = DataRecordProcessConfig.Priority(1)
        ),
        DataRecordProcessConfig.PriorityByAmountConfig(
          amountRange = DataRecordProcessConfig.AmountRange(from = BigDecimal(501), to = None),
          priority = DataRecordProcessConfig.Priority(2)
        )
      )
    )

    val expectedDocument = DataRecordProcessConfigDocument(
      _id = DataRecordProcessConfigMongoDocs.dataRecordProcessConfigId,
      prioritiesByAmount = Seq(
        DataRecordProcessConfigMongoDocs.PriorityByAmountConfigDocument(BigDecimal(0), Some(BigDecimal(500)), 1),
        DataRecordProcessConfigMongoDocs.PriorityByAmountConfigDocument(BigDecimal(501), None, 2)
      )
    )

    val result = DataRecordProcessConfigMapper.toDocument(config)
    result shouldBe expectedDocument
  }

  "toDomain" should "convert DataRecordProcessConfigDocument to DataRecordProcessConfig" in {
    val document = DataRecordProcessConfigDocument(
      DataRecordProcessConfigMongoDocs.dataRecordProcessConfigId,
      Seq(
        DataRecordProcessConfigMongoDocs.PriorityByAmountConfigDocument(BigDecimal(0), Some(BigDecimal(500)), 1),
        DataRecordProcessConfigMongoDocs.PriorityByAmountConfigDocument(BigDecimal(501), None, 2)
      )
    )

    val expectedConfig = DataRecordProcessConfig(
      prioritiesByAmount = Set(
        DataRecordProcessConfig.PriorityByAmountConfig(
          amountRange = DataRecordProcessConfig.AmountRange(from = BigDecimal(0), to = Some(BigDecimal(500))),
          priority = DataRecordProcessConfig.Priority(1)
        ),
        DataRecordProcessConfig.PriorityByAmountConfig(
          amountRange = DataRecordProcessConfig.AmountRange(from = BigDecimal(501), to = None),
          priority = DataRecordProcessConfig.Priority(2)
        )
      )
    )

    val result = DataRecordProcessConfigMapper.toDomain(document)
    result shouldBe expectedConfig
  }
}
