package com.mp.infrastructure.configuration.mongo

import reactivemongo.api.bson.{BSONArray, BSONDateTime, BSONDecimal, BSONDocument, BSONString, BSONValue, ElementProducer}
import reactivemongo.api.commands.WriteResult
import MongoClient.MongoWriteException

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

trait MongoAdapters {
  protected implicit class MongoWriteExt(future: Future[WriteResult]) {
    def throwErrors(implicit ec: ExecutionContext): Future[Unit] =
      future.map(
        result =>
          if (result.writeErrors.isEmpty) ()
          else throw MongoWriteException(result))
  }

  protected implicit def toBson(string: String): BSONString =
    BSONString(string)

  protected implicit def toBson(bigDecimal: BigDecimal): BSONDecimal =
    BSONDecimal.fromBigDecimal(bigDecimal).get

  protected implicit def toBson(seq: Seq[BSONValue]): BSONArray =
    BSONArray(seq)

  protected implicit def toBson(instant: Instant): BSONDateTime =
    BSONDateTime(instant.toEpochMilli)

  protected def bson(elms: ElementProducer*): BSONDocument =
    BSONDocument(elms: _*)

  protected def bsonId[V](id: V)(implicit toBson: V => BSONValue): BSONDocument =
    BSONDocument("_id" -> toBson(id))

  protected def bsonBetween[V](from: Option[V], to: Option[V])(implicit
                                                               toBson: V => BSONValue
  ): Option[BSONDocument] = {
    (from, to) match {
      case (None, None)           => None
      case (Some(from), None)     => Some(BSONDocument("$gte" -> toBson(from)))
      case (None, Some(to))       => Some(BSONDocument("$lte" -> toBson(to)))
      case (Some(from), Some(to)) => Some(BSONDocument("$gte" -> toBson(from), "$lte" -> toBson(to)))
    }
  }

  protected def bsonSet(document: BSONDocument): BSONDocument =
    BSONDocument("$set" -> document)
}
