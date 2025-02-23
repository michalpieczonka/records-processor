package com.mp

import akka.actor.ActorSystem
import com.mp.app.task.DefaultDataRecordInitializer
import com.mp.domain.data_record.DataRecordService
import com.mp.domain.data_record.process_configuration.DataRecordProcessConfigService
import com.mp.domain.data_record.report.DataRecordReportFactory
import com.mp.domain.shared.id.UuidFactory
import com.mp.infrastructure.configuration.http.HttpServer
import com.mp.infrastructure.configuration.mongo.MongoClient
import com.mp.infrastructure.persistence.data_record.DataRecordMongoRepository
import com.mp.infrastructure.persistence.data_record.process_configuration.DataRecordProcessConfigMongoRepository
import com.mp.infrastructure.rest.data_record.DataRecordEndpoint
import com.mp.infrastructure.rest.data_record.process_configuration.DataRecordProcessConfigEndpoint
import com.typesafe.scalalogging.StrictLogging

import java.time.Clock

class Application(config: AppConfig) extends StrictLogging {
  implicit val system     = ActorSystem("MpRankomatApplication", config.raw)
  implicit val dispatcher = system.dispatcher
  implicit val clock      = Clock.system(config.timeZone)

  private implicit val mongoClient = new MongoClient(config.mongoConfig)

  private implicit val idFactory: UuidFactory = new UuidFactory

  private implicit val dataRecordProcessConfigRepository: DataRecordProcessConfigMongoRepository = new DataRecordProcessConfigMongoRepository(mongoClient)
  private implicit val dataRecordProcessConfigService: DataRecordProcessConfigService = new DataRecordProcessConfigService

  private implicit val dataRecordRepository: DataRecordMongoRepository = new DataRecordMongoRepository(mongoClient)
  private implicit val dataRecordReportFactory: DataRecordReportFactory = new DataRecordReportFactory
  private implicit val dataRecordService: DataRecordService = new DataRecordService

  val httpServer = new HttpServer(
    config = config.httpServerConfig,
    endpoints = Seq(
      new DataRecordProcessConfigEndpoint,
      new DataRecordEndpoint
    )
  )

  def start(): Unit = {

    new DefaultDataRecordInitializer(config.dataInitializeConfig).initialize()
    dataRecordProcessConfigService.initializeCache()
    httpServer.start()
  }
}
