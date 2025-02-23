package com.mp.domain.data_record

import com.mp.domain.data_record.process_configuration.DataRecordProcessConfig.{
  AmountRange,
  Priority,
  PriorityByAmountConfig
}
import com.mp.domain.data_record.process_configuration.{DataRecordProcessConfig, DataRecordProcessConfigService}
import com.mp.domain.shared.id.UuidFactory
import com.mp.domain.shared.{Amount, Name, PhoneNumber}
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.{ExecutionContext, Future}

class DataRecordServiceTest extends AnyWordSpec with Matchers with ScalaFutures with MockitoSugar {
  implicit val ec: ExecutionContext = ExecutionContext.global

  "DataRecordService" should {
    "create a new record successfully" in {
      implicit val mockRepository    = mock[DataRecordRepository]
      implicit val mockIdFactory     = mock[UuidFactory]
      implicit val mockClock         = Clock.fixed(Instant.parse("2024-01-01T12:00:00Z"), ZoneId.of("UTC"))
      implicit val mockConfigService = mock[DataRecordProcessConfigService]

      val recordId = DataRecordId("test-id")
      when(mockIdFactory.generate()).thenReturn("test-id")
      when(mockRepository.save(any[DataRecord])).thenReturn(Future.successful(()))

      val service = new DataRecordService
      val command = DataRecordCommand.Create(Name("John Doe"), PhoneNumber("123456789"), Amount(100))

      whenReady(service.createRecord(command)) { result =>
        result shouldBe recordId
        verify(mockRepository).save(any[DataRecord])
      }
    }

    "process a record successfully when one is available" in {
      implicit val mockRepository    = mock[DataRecordRepository]
      implicit val mockIdFactory     = mock[UuidFactory]
      implicit val mockClock         = Clock.fixed(Instant.parse("2024-01-01T12:00:00Z"), ZoneId.of("UTC"))
      implicit val mockConfigService = mock[DataRecordProcessConfigService]

      val nowTime = Instant.parse("2024-01-01T12:00:00Z")
      val record = DataRecord(
        DataRecordId("test-id"),
        Name("John Doe"),
        PhoneNumber("123456789"),
        Amount(100),
        nowTime.minusSeconds(3600),
        None
      )

      val processConfig = DataRecordProcessConfig(
        Set(PriorityByAmountConfig(AmountRange(BigDecimal(50), Some(BigDecimal(200))), Priority(1)))
      )
      when(mockConfigService.get()).thenReturn(Future.successful(processConfig))
      when(mockRepository.getNextRecordToProcess(any[DataRecordProcessCriteria]))
        .thenReturn(Future.successful(Some(record)))
      when(mockRepository.save(any[DataRecord])).thenReturn(Future.successful(()))

      val service = new DataRecordService

      whenReady(service.processRecord()) { result =>
        result shouldBe defined
        result.get.processTime shouldBe Some(nowTime)
        verify(mockRepository).save(any[DataRecord])
      }
    }

    "return None when no records are available to process" in {
      implicit val mockRepository    = mock[DataRecordRepository]
      implicit val mockIdFactory     = mock[UuidFactory]
      implicit val mockClock         = Clock.fixed(Instant.parse("2024-01-01T12:00:00Z"), ZoneId.of("UTC"))
      implicit val mockConfigService = mock[DataRecordProcessConfigService]

      val processConfig = DataRecordProcessConfig(
        Set(PriorityByAmountConfig(AmountRange(BigDecimal(50), Some(BigDecimal(200))), Priority(1)))
      )
      when(mockConfigService.get()).thenReturn(Future.successful(processConfig))
      when(mockRepository.getNextRecordToProcess(any[DataRecordProcessCriteria])).thenReturn(Future.successful(None))

      val service = new DataRecordService

      whenReady(service.processRecord()) { result =>
        result shouldBe None
        verify(mockRepository, never).save(any[DataRecord])
      }
    }
  }
}
