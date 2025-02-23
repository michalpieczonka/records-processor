package com.mp.infrastructure.rest

import com.mp.domain.data_record.report.DataRecordReport
import com.mp.domain.data_record.{DataRecord, DataRecordId}
import com.mp.domain.shared._
import com.mp.infrastructure.rest.data_record.{DataRecordApi, DataRecordApiMapper}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.mockito.Mockito._

import java.time.{Clock, Instant, ZoneId}

class DataRecordApiMapperTest extends AnyFunSuite with Matchers {

  test("toApi should correctly map DataRecord to ProcessedRecord") {
    val record = DataRecord(
      id = DataRecordId("123"),
      name = Name("John Doe"),
      phoneNumber = PhoneNumber("123456789"),
      amount = Amount(BigDecimal(100.5)),
      createTime = Instant.parse("2024-01-01T12:00:00Z"),
      processTime = None
    )

    val expected = DataRecordApi.GetDataRecordResponse.ProcessedRecord(
      dataRecordId = "123",
      name = "John Doe",
      phoneNumber = "123456789",
      amount = 100.5,
      createTime = Instant.parse("2024-01-01T12:00:00Z")
    )

    val result = DataRecordApiMapper.toApi(record)
    result shouldBe expected
  }

  test("toApi should correctly map DataRecordReport to GetDataRecordReport") {
    val fixedInstant = Instant.parse("2024-02-20T15:30:00Z")
    val mockClock = mock(classOf[Clock])
    when(mockClock.instant()).thenReturn(fixedInstant)
    when(mockClock.getZone).thenReturn(ZoneId.of("UTC"))

    val report = DataRecordReport(
      entries = List(
        DataRecordReport.Entry(
          phoneNumber = "123456789",
          records = List(
            DataRecordReport.Entry.Record(
              name = "John Doe",
              recordsAmountSum = BigDecimal(500.75),
              recordsNewestCreateTime = fixedInstant
            )
          )
        )
      )
    )

    val expected = DataRecordApi.GetDataRecordReport(
      entries = Seq(
        DataRecordApi.GetDataRecordReport.Entry(
          phoneNumber = "123456789",
          records = Seq(
            DataRecordApi.GetDataRecordReport.RecordData(
              name = "John Doe",
              content = DataRecordApi.GetDataRecordReport.Content(
                amountsSum = BigDecimal(500.75),
                newestRecordCreateDate = fixedInstant.atZone(ZoneId.of("UTC")).toLocalDate
              )
            )
          )
        )
      )
    )

    val result = DataRecordApiMapper.toApi(report)(mockClock)
    result shouldBe expected
  }
}