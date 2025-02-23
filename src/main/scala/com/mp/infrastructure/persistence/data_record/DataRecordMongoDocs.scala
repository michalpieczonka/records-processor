package com.mp.infrastructure.persistence.data_record

import reactivemongo.api.bson.{BSONDocumentHandler, Macros}

import java.time.Instant

object DataRecordMongoDocs {
  implicit val dataRecordDocumentHandler: BSONDocumentHandler[DataRecordDocument] = Macros.handler[DataRecordDocument]

  case class DataRecordDocument(
      _id: String,
      name: String,
      phoneNumber: String,
      amount: BigDecimal,
      createTime: Instant,
      processTime: Option[Instant]
  )
}
