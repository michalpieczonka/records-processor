package com.mp.infrastructure.configuration.mongo

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import com.typesafe.scalalogging.StrictLogging
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.bson.collection.BSONSerializationPack.{NarrowValueReader, Reader, Writer}
import reactivemongo.api.bson.{BSONDocument, BSONValue}
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.{Index, IndexType}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class MongoConfiguredRepository[DOC](
    mongoClient: MongoClient,
    collectionName: String
)(implicit ec: ExecutionContext, system: ActorSystem)
    extends MongoAdapters
    with StrictLogging {
  import MongoConfiguredRepository._

  protected def collection: Future[BSONCollection] = mongoClient.collection(collectionName)

  def drop(): Future[Unit] = {
    mongoClient
      .collection(collectionName)
      .flatMap(_.drop(failIfNotFound = false))
      .map(dropped => if (dropped) logger.warn(s"Successfully dropped collection, collectionName=$collectionName"))
  }

  protected def indexEnsure(index: DbIndex): Unit =
    indexEnsure(
      DbCompoundIndex(idxName = index.idxName, fields = Seq(index.fieldName -> index.idxType), unique = index.unique)
    )

  protected def indexEnsure(dbCompoundIndex: DbCompoundIndex): Unit = {
    implicit val scheduler = system.scheduler

    akka.pattern.retry(
      () => collection.flatMap(_.indexesManager.ensure(dbCompoundIndex.toIndex)),
      10,
      60.seconds
    )
    collection
      .flatMap(_.indexesManager.ensure(dbCompoundIndex.toIndex))
      .onComplete {
        case Failure(ex) =>
          logger.error(s"MongoRepository index creation failed, collectionName=$collectionName", ex)
        case Success(created) if created =>
          logger.info(s"MongoRepository index created, collectionName=$collectionName")
        case Success(_) => ()
      }
  }

  protected def findOneById[ID](id: ID)(implicit
      reader: Reader[DOC],
      toBson: ID => BSONValue
  ): Future[Option[DOC]] =
    findOne(bsonId(id))

  protected def findOne(selector: BSONDocument)(implicit
      reader: Reader[DOC]
  ): Future[Option[DOC]] =
    collection.flatMap(
      _.find(selector)
        .one[DOC]
    )

  protected def findMany(
      selector: BSONDocument,
      sort: Option[BSONDocument] = None,
      limit: Int = Int.MaxValue
  )(implicit reader: Reader[DOC]): Future[Vector[DOC]] =
    collection.flatMap(
      _.find(selector)
        .sort(sort.getOrElse(BSONDocument.empty))
        .cursor[DOC]()
        .collect[Vector](maxDocs = limit)
    )

  protected def findDistinct[T](
      fieldName: String,
      selector: Option[BSONDocument] = None
  )(implicit reader: NarrowValueReader[T]): Future[Vector[T]] =
    collection.flatMap(_.distinct[T, Vector](fieldName, selector))

  protected def findSource(
      selector: BSONDocument,
      sort: Option[BSONDocument] = None
  )(implicit reader: Reader[DOC]): Source[DOC, Unit] = {
    Source
      .futureSource(
        collection.map(
          _.find(selector)
            .sort(sort.getOrElse(BSONDocument.empty))
            .cursor[DOC]()
            .documentSource()
        )
      )
      .mapMaterializedValue(_ => ())
  }

  protected def insertOne(document: DOC)(implicit writer: Writer[DOC]): Future[WriteResult] =
    collection.flatMap(_.insert.one[DOC](document))

  protected def upsertOneById[ID](id: ID, document: DOC)(implicit
      writer: Writer[DOC],
      toBson: ID => BSONValue
  ): Future[WriteResult] =
    upsertOne(bsonId(id), document)

  protected def upsertOne(query: BSONDocument, update: BSONDocument): Future[WriteResult] =
    collection.flatMap(_.update.one(query, update, upsert = true))

  protected def upsertOne(query: BSONDocument, document: DOC)(implicit
      writer: Writer[DOC]
  ): Future[WriteResult] =
    collection.flatMap(_.update.one(query, document, upsert = true))

}
object MongoConfiguredRepository {
  case class DbIndex(
      idxName: String,
      fieldName: String,
      idxType: IndexType,
      unique: Boolean = false
  ) {
    def toIndex =
      Index(
        key = Seq(fieldName -> idxType),
        name =  Some(idxName),
        unique = unique
      )
  }

  case class DbCompoundIndex(
      idxName: String,
      fields: Seq[(String, IndexType)],
      unique: Boolean = false
  ) {
    def toIndex =
      Index(
        key = fields,
        name = Some(idxName),
        unique = unique
      )
  }
}
