package com.mp.infrastructure.persistence.data_record.process_configuration

import akka.actor.ActorSystem
import com.mp.domain.data_record.process_configuration.{DataRecordProcessConfig, DataRecordProcessConfigRepository}
import com.mp.infrastructure.configuration.mongo.{MongoClient, MongoConfiguredRepository}

import scala.concurrent.{ExecutionContext, Future}

class DataRecordProcessConfigMongoRepository(mongoClient: MongoClient)(implicit
    ec: ExecutionContext,
    system: ActorSystem
) extends MongoConfiguredRepository[DataRecordProcessConfigMongoDocs.DataRecordProcessConfigDocument](
      mongoClient = mongoClient,
      collectionName = "data_record_process_configuration"
    )
    with DataRecordProcessConfigRepository {
  import DataRecordProcessConfigMongoDocs._

  override def save(processConfig: DataRecordProcessConfig): Future[Unit] = {
    val document = DataRecordProcessConfigMapper.toDocument(processConfig)
    upsertOneById(dataRecordProcessConfigId, document).throwErrors
  }

  override def get(): Future[Option[DataRecordProcessConfig]] = {
    findOneById(dataRecordProcessConfigId).map(
      _.map(DataRecordProcessConfigMapper.toDomain)
    )
  }
}
