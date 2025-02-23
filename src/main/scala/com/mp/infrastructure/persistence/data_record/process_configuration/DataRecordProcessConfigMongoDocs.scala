package com.mp.infrastructure.persistence.data_record.process_configuration

import reactivemongo.api.bson.{BSONDocumentHandler, Macros}

object DataRecordProcessConfigMongoDocs {
  val dataRecordProcessConfigId = "DATA_REPORT_PROCESS_CONFIG"

  implicit val priorityByAmountConfigDocumentHandler: BSONDocumentHandler[PriorityByAmountConfigDocument] =
    Macros.handler[PriorityByAmountConfigDocument]
  implicit val dataRecordProcessConfigDocumentHandler: BSONDocumentHandler[DataRecordProcessConfigDocument] =
    Macros.handler[DataRecordProcessConfigDocument]

  case class DataRecordProcessConfigDocument(
      _id: String,
      prioritiesByAmount: Seq[PriorityByAmountConfigDocument]
  )

  case class PriorityByAmountConfigDocument(
      amountRangeFrom: BigDecimal,
      amountRangeTo: Option[BigDecimal],
      priority: Int
  )
}
