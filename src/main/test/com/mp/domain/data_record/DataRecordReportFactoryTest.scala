package com.mp.domain.data_record

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import com.mp.domain.data_record.report.{DataRecordReportCriteria, DataRecordReportFactory}
import com.mp.domain.shared._
import org.mockito.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.Instant
import scala.concurrent.ExecutionContext

class DataRecordReportFactoryTest  extends AnyWordSpec with Matchers with ScalaFutures with MockitoSugar {
  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val system: ActorSystem = ActorSystem("TestSystem")

  "DataRecordReportFactory" should {
    "generate a report including all records" in {
      implicit val mockRepository = mock[DataRecordRepository]

      val factory = new DataRecordReportFactory()

      val record1 = DataRecord(DataRecordId("id-1"), Name("John Doe"), PhoneNumber("123456789"), Amount(BigDecimal(200)), Instant.parse("2024-01-01T12:00:00Z"), Some(Instant.parse("2024-01-01T12:00:00Z")))
      val record2 = DataRecord(DataRecordId("id-2"), Name("John Doe"), PhoneNumber("123456789"), Amount(BigDecimal(120)), Instant.parse("2024-01-01T17:00:00Z"), Some(Instant.parse("2024-01-01T18:00:00Z")))
      val record3 = DataRecord(DataRecordId("id-3"), Name("Alice"), PhoneNumber("123456789"), Amount(BigDecimal(100)), Instant.parse("2025-01-01T12:00:00Z"), None)
      val record4 = DataRecord(DataRecordId("id-4"), Name("Bob"), PhoneNumber("987654321"), Amount(BigDecimal(80)), Instant.parse("2024-01-01T12:00:00Z"), Some(Instant.parse("2025-01-01T12:00:00Z")))
      val record5 = DataRecord(DataRecordId("id-5"), Name("Charlie"), PhoneNumber("999654321"), Amount(BigDecimal(50)), Instant.parse("2024-01-03T12:00:00Z"), Some(Instant.parse("2025-01-01T12:00:00Z")))
      val record6 = DataRecord(DataRecordId("id-6"), Name("Burton"), PhoneNumber("999654321"), Amount(BigDecimal(30)), Instant.parse("2021-01-03T12:00:00Z"), None)

      val source = Source(List(record1, record2, record3, record4, record5, record6))
      val criteria = DataRecordReportCriteria(onlyProcessedRecords = false)
      when(mockRepository.getSourceForReport(criteria)).thenReturn(source.mapMaterializedValue(_ => ()))


      whenReady(factory.generateRecordsReport(criteria)) { report =>
        report.entries should have size 3
        val entry1 = report.entries.find(_.phoneNumber == "123456789").get
        entry1.records should have size 2
        entry1.records.filter(_.name == "John Doe").map(_.recordsAmountSum).sum shouldBe 320
        entry1.records.filter(_.name == "John Doe").map(_.recordsNewestCreateTime).head shouldBe Instant.parse("2024-01-01T17:00:00Z")
        entry1.records.filter(_.name == "Alice").map(_.recordsAmountSum).sum shouldBe 100
        entry1.records.filter(_.name == "Alice").map(_.recordsNewestCreateTime).head shouldBe Instant.parse("2025-01-01T12:00:00Z")

        val entry2 = report.entries.find(_.phoneNumber == "987654321").get
        entry2.records should have size 1
        entry2.records.head.recordsAmountSum shouldBe 80
        entry2.records.head.recordsNewestCreateTime shouldBe Instant.parse("2024-01-01T12:00:00Z")

        val entry3 = report.entries.find(_.phoneNumber == "999654321").get
        entry3.records should have size 2
        entry3.records.filter(_.name == "Charlie").map(_.recordsAmountSum).sum shouldBe 50
        entry3.records.filter(_.name == "Charlie").map(_.recordsNewestCreateTime).head shouldBe Instant.parse("2024-01-03T12:00:00Z")
        entry3.records.filter(_.name == "Burton").map(_.recordsAmountSum).sum shouldBe 30
        entry3.records.filter(_.name == "Burton").map(_.recordsNewestCreateTime).head shouldBe Instant.parse("2021-01-03T12:00:00Z")
      }
    }

    "generate a report including only processed records" in {
      implicit val mockRepository = mock[DataRecordRepository]

      val factory = new DataRecordReportFactory()

      val record1 = DataRecord(DataRecordId("id-1"), Name("John Doe"), PhoneNumber("123456789"), Amount(BigDecimal(200)), Instant.parse("2024-01-01T12:00:00Z"), Some(Instant.parse("2024-01-01T12:00:00Z")))
      val record2 = DataRecord(DataRecordId("id-2"), Name("John Doe"), PhoneNumber("123456789"), Amount(BigDecimal(120)), Instant.parse("2024-01-01T17:00:00Z"), Some(Instant.parse("2024-01-01T18:00:00Z")))
      val record3 = DataRecord(DataRecordId("id-3"), Name("Alice"), PhoneNumber("123456789"), Amount(BigDecimal(100)), Instant.parse("2025-01-01T12:00:00Z"), None)
      val record4 = DataRecord(DataRecordId("id-4"), Name("Bob"), PhoneNumber("987654321"), Amount(BigDecimal(80)), Instant.parse("2024-01-01T12:00:00Z"), Some(Instant.parse("2025-01-01T12:00:00Z")))
      val record5 = DataRecord(DataRecordId("id-5"), Name("Charlie"), PhoneNumber("999654321"), Amount(BigDecimal(50)), Instant.parse("2024-01-03T12:00:00Z"), None)
      val record6 = DataRecord(DataRecordId("id-6"), Name("Burton"), PhoneNumber("999654321"), Amount(BigDecimal(30)), Instant.parse("2021-01-03T12:00:00Z"), None)

      val source = Source(List(record1, record2, record4))
      val criteria = DataRecordReportCriteria(onlyProcessedRecords = true)
      when(mockRepository.getSourceForReport(criteria)).thenReturn(source.mapMaterializedValue(_ => ()))


      whenReady(factory.generateRecordsReport(criteria)) { report =>
        report.entries should have size 2
        val entry1 = report.entries.find(_.phoneNumber == "123456789").get
        entry1.records should have size 1
        entry1.records.filter(_.name == "John Doe").map(_.recordsAmountSum).sum shouldBe 320
        entry1.records.filter(_.name == "John Doe").map(_.recordsNewestCreateTime).head shouldBe Instant.parse("2024-01-01T17:00:00Z")

        val entry2 = report.entries.find(_.phoneNumber == "987654321").get
        entry2.records should have size 1
        entry2.records.head.recordsAmountSum shouldBe 80
        entry2.records.head.recordsNewestCreateTime shouldBe Instant.parse("2024-01-01T12:00:00Z")
      }
    }
  }
}
