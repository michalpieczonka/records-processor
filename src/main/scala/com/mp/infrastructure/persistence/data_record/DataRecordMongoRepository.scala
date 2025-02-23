package com.mp.infrastructure.persistence.data_record

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import com.mp.domain.data_record.report.DataRecordReportCriteria
import com.mp.domain.data_record.{DataRecord, DataRecordProcessCriteria, DataRecordRepository}
import com.mp.infrastructure.configuration.mongo.MongoConfiguredRepository.{DbCompoundIndex, DbIndex}
import com.mp.infrastructure.configuration.mongo.{MongoClient, MongoConfiguredRepository}
import com.mp.infrastructure.persistence.data_record.DataRecordMongoDocs.DataRecordDocument
import reactivemongo.api.bson.{BSONArray, BSONDocument, BSONInteger, BSONNull, BSONString}
import reactivemongo.api.indexes.IndexType

import scala.concurrent.{ExecutionContext, Future}

class DataRecordMongoRepository(mongoClient: MongoClient)(implicit ec: ExecutionContext, system: ActorSystem)
    extends MongoConfiguredRepository[DataRecordDocument](
      mongoClient = mongoClient,
      collectionName = "data_record"
    )
    with DataRecordRepository {
  createIndexes()

  override def save(dataRecord: DataRecord): Future[Unit] = {
    val document = DataRecordMapper.toDocument(dataRecord)
    upsertOneById(document._id, document).throwErrors
  }

  def getNextRecordToProcess(criteria: DataRecordProcessCriteria): Future[Option[DataRecord]] = {
    if (criteria.priorityByAmountConfig.isEmpty) {
      logger.warn("No priorityByAmountConfig provided, returning latest saved record")
      getNextRecordWithoutPriority(criteria)
    } else {
      getNextRecordWithPriority(criteria)
    }
  }

  override def getSourceForReport(criteria: DataRecordReportCriteria): Source[DataRecord, Unit] = {
    findSource(
      selector = bson(
        if (criteria.onlyProcessedRecords) {
          "processTime" -> bson("$ne" -> BSONNull)
        } else {
          BSONDocument.empty
        }
      )
    ).map(DataRecordMapper.toDomain).mapMaterializedValue(_ => ())
  }

  private def createIndexes(): Unit = {
    indexEnsure(DbIndex(idxName = "idx-amount", fieldName = "amount", IndexType.Descending))
    indexEnsure(
      DbCompoundIndex(
        idxName = "idx-phone_number-process_time",
        fields = Seq("phoneNumber" -> IndexType.Ascending, "processTime" -> IndexType.Descending)
      )
    )
    indexEnsure(
      DbCompoundIndex(
        idxName = "idx-phone_number-amount",
        fields = Seq("phoneNumber" -> IndexType.Ascending, "amount" -> IndexType.Descending)
      )
    )
  }

  private def getNextRecordWithoutPriority(criteria: DataRecordProcessCriteria): Future[Option[DataRecord]] = {
    collection.flatMap { col =>
      col
        .aggregateWith[DataRecordDocument]() { framework =>
          val lookupStage = framework.Lookup(
            from = "data_record",
            localField = "phoneNumber",
            foreignField = "phoneNumber",
            as = "recentlyProcessed"
          )

          val matchStage = framework.Match(
            BSONDocument(
              "processTime" -> BSONNull,
              "$or" -> BSONArray(
                BSONDocument("recentlyProcessed" -> BSONArray()),
                BSONDocument(
                  "$expr" -> BSONDocument(
                    "$eq" -> BSONArray(
                      BSONDocument(
                        "$size" -> BSONDocument(
                          "$filter" -> BSONDocument(
                            "input" -> "$recentlyProcessed",
                            "as"    -> "rp",
                            "cond" -> BSONDocument(
                              "$and" -> BSONArray(
                                BSONDocument(
                                  "$ne" -> BSONArray(BSONString("$$rp.processTime"), BSONNull)
                                ),
                                BSONDocument(
                                  "$gte" -> BSONArray(
                                    BSONString("$$rp.processTime"),
                                    criteria.unAllowedProcessEarliestTimeWithSamePhoneNumber
                                  )
                                )
                              )
                            )
                          )
                        )
                      ),
                      0
                    )
                  )
                )
              )
            )
          )

          val sortStage = framework.Sort(framework.Descending("createdTime"))

          val aggregationPipeline = List(lookupStage, matchStage, sortStage, framework.Limit(1))

          aggregationPipeline
        }
        .headOption
        .map(_.map(DataRecordMapper.toDomain))
    }
  }

  private def getNextRecordWithPriority(criteria: DataRecordProcessCriteria): Future[Option[DataRecord]] = {
    collection.flatMap { col =>
      col
        .aggregateWith[DataRecordDocument]() { framework =>
          val priorityCases = criteria.priorityByAmountConfig.map { range =>
            val amountFrom = range.amountRange.from
            val amountTo   = range.amountRange.to.getOrElse(BigDecimal.valueOf(Long.MaxValue))

            BSONDocument(
              "case" -> BSONDocument(
                "$and" -> BSONArray(
                  BSONDocument("$gte" -> BSONArray("$amount", amountFrom)),
                  BSONDocument("$lte" -> BSONArray("$amount", amountTo))
                )
              ),
              "then" -> BSONInteger(range.priority.value)
            )
          }

          val lookupStage = framework.Lookup(
            from = "data_record",
            localField = "phoneNumber",
            foreignField = "phoneNumber",
            as = "recentlyProcessed"
          )

          val addFieldsStage = framework.AddFields(
            BSONDocument(
              "priority" -> BSONDocument(
                "$switch" -> BSONDocument(
                  "branches" -> BSONArray(priorityCases),
                  "default"  -> BSONInteger(Int.MaxValue)
                )
              )
            )
          )

          val matchStage = framework.Match(
            BSONDocument(
              "processTime" -> BSONNull,
              "$or" -> BSONArray(
                BSONDocument("recentlyProcessed" -> BSONArray()),
                BSONDocument(
                  "$expr" -> BSONDocument(
                    "$eq" -> BSONArray(
                      BSONDocument(
                        "$size" -> BSONDocument(
                          "$filter" -> BSONDocument(
                            "input" -> "$recentlyProcessed",
                            "as"    -> "rp",
                            "cond" -> BSONDocument(
                              "$and" -> BSONArray(
                                BSONDocument(
                                  "$ne" -> BSONArray(BSONString("$$rp.processTime"), BSONNull)
                                ),
                                BSONDocument(
                                  "$gte" -> BSONArray(
                                    BSONString("$$rp.processTime"),
                                    criteria.unAllowedProcessEarliestTimeWithSamePhoneNumber
                                  )
                                )
                              )
                            )
                          )
                        )
                      ),
                      0
                    )
                  )
                )
              )
            )
          )

          val sortStage = framework.Sort(
            framework.Descending("priority"),
            framework.Descending("amount")
          )

          val aggregationPipeline = List(
            lookupStage,
            addFieldsStage,
            matchStage,
            sortStage,
            framework.Limit(1)
          )

          aggregationPipeline
        }
        .headOption
        .map(_.map(DataRecordMapper.toDomain))
    }
  }
}
