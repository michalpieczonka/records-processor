package com.mp.infrastructure.persistence.data_record

import com.mp.domain.data_record.{DataRecord, DataRecordId}
import com.mp.domain.shared._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.Instant

class DataRecordMapperTest extends AnyFlatSpec with Matchers {

  "toDocument" should "convert DataRecord to DataRecordDocument" in {
    val dataRecord = DataRecord(
      id = DataRecordId("12345"),
      name = Name("John Doe"),
      phoneNumber = PhoneNumber("123456789"),
      amount = Amount(100.50),
      createTime = Instant.ofEpochMilli(1678901234L),
      processTime = Some(Instant.ofEpochMilli(1678904567L))
    )

    val expectedDocument = DataRecordMongoDocs.DataRecordDocument(
      _id = "12345",
      name = "John Doe",
      phoneNumber = "123456789",
      amount = 100.50,
      createTime = Instant.ofEpochMilli(1678901234L),
      processTime = Some(Instant.ofEpochMilli(1678904567L))
    )

    val result = DataRecordMapper.toDocument(dataRecord)
    result shouldBe expectedDocument
  }

  "toDomain" should "convert DataRecordDocument to DataRecord" in {
    val document = DataRecordMongoDocs.DataRecordDocument(
      _id = "12345",
      name = "John Doe",
      phoneNumber = "123456789",
      amount = 100.50,
      createTime = Instant.ofEpochMilli(1678901234L),
      processTime = Some(Instant.ofEpochMilli(1678904567L))
    )

    val expectedDataRecord = DataRecord(
      id = DataRecordId("12345"),
      name = Name("John Doe"),
      phoneNumber = PhoneNumber("123456789"),
      amount = Amount(100.50),
      createTime = Instant.ofEpochMilli(1678901234L),
      processTime = Some(Instant.ofEpochMilli(1678904567L))
    )

    val result = DataRecordMapper.toDomain(document)
    result shouldBe expectedDataRecord
  }
}
