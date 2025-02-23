package com.mp.app.task

import com.mp.domain.data_record.process_configuration.{DataRecordProcessConfig, DataRecordProcessConfigRepository}
import com.mp.domain.data_record.{DataRecord, DataRecordId, DataRecordRepository}
import com.mp.domain.shared.{Amount, Name, PhoneNumber}
import com.mp.domain.shared.id.UuidFactory
import com.typesafe.scalalogging.StrictLogging

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

//Class only for manual testing purposes. Preferably i would move it to test classes and use only for integration tests
//to simplify i keep it here as standalone class accessible also for application
class DefaultDataRecordInitializer(config: DefaultDataRecordInitializer.Config)(implicit
    ec: ExecutionContext,
    dataRecordRepository: DataRecordRepository,
    dataRecordProcessConfigRepository: DataRecordProcessConfigRepository,
    idFactory: UuidFactory
) extends StrictLogging {

  def initialize(): Unit = {
    if (config.initializeConfigOnStartup) {
      initializeConfig()
    }
    if (config.initializeRecordsOnStartup) {
      initializeData()
    }
    logger.info("Data record startup initialization completed")
  }

  def initializeData(): Unit = {
    val records = Seq(
      DataRecord(
        id = DataRecordId(idFactory.generate()),
        name = Name("John Doe"),
        phoneNumber = PhoneNumber("1234567890"),
        amount = Amount(BigDecimal(9000.125)),
        createTime = Instant.parse("2025-02-22T00:00:00Z"),
        processTime = None
      ),
      DataRecord(
        id = DataRecordId(idFactory.generate()),
        name = Name("Jane Doe"),
        phoneNumber = PhoneNumber("1234567890"),
        amount = Amount(BigDecimal(1491.50)),
        createTime = Instant.parse("2025-02-22T00:00:00Z"),
        processTime = Some(Instant.parse("2025-02-23T00:00:00Z"))
      ),
      DataRecord(
        id = DataRecordId(idFactory.generate()),
        name = Name("John Smith"),
        phoneNumber = PhoneNumber("0987654321"),
        amount = Amount(BigDecimal(3500.00)),
        createTime = Instant.parse("2025-02-22T00:00:00Z"),
        processTime = None
      ),
      DataRecord(
        id = DataRecordId(idFactory.generate()),
        name = Name("Jane Smith"),
        phoneNumber = PhoneNumber("0987654321"),
        amount = Amount(BigDecimal(200.00)),
        createTime = Instant.parse("2025-02-23T00:00:00Z"),
        processTime = None
      ),
      DataRecord(
        id = DataRecordId(idFactory.generate()),
        name = Name("Adam Smith"),
        phoneNumber = PhoneNumber("1234567890"),
        amount = Amount(BigDecimal(1255.00)),
        createTime = Instant.parse("2025-02-23T00:00:00Z"),
        processTime = None
      )
    )

    Future.sequence(records.map(dataRecordRepository.save)).map(_ => ()).onComplete {
      case Success(_)         => logger.info("Data records initialized successfully")
      case Failure(exception) => logger.error(s"Data records initialization failed: ${exception.getMessage}")
    }
  }

  private def initializeConfig(): Unit = {
    val defaultConfig = DataRecordProcessConfig(
      prioritiesByAmount = Set(
        DataRecordProcessConfig.PriorityByAmountConfig(
          amountRange = DataRecordProcessConfig.AmountRange(from = BigDecimal(0), to = Some(BigDecimal(500))),
          priority = DataRecordProcessConfig.Priority(1)
        ),
        DataRecordProcessConfig.PriorityByAmountConfig(
          amountRange = DataRecordProcessConfig.AmountRange(from = BigDecimal(501), to = Some(BigDecimal(3000))),
          priority = DataRecordProcessConfig.Priority(2)
        ),
        DataRecordProcessConfig.PriorityByAmountConfig(
          amountRange = DataRecordProcessConfig.AmountRange(from = BigDecimal(3001), to = None),
          priority = DataRecordProcessConfig.Priority(3)
        )
      )
    )

    dataRecordProcessConfigRepository.save(defaultConfig).onComplete {
      case Success(_) => logger.info("Data record process config initialized successfully")
      case Failure(exception) =>
        logger.error(s"Data record process config initialization failed: ${exception.getMessage}")
    }
  }
}

object DefaultDataRecordInitializer {
  case class Config(
      initializeRecordsOnStartup: Boolean,
      initializeConfigOnStartup: Boolean
  )
}
