package com.mp

import com.mp.app.task.DefaultDataRecordInitializer
import com.mp.infrastructure.configuration.http.HttpServer
import com.mp.infrastructure.configuration.mongo.MongoClient
import com.typesafe.config.{Config, ConfigFactory}

import java.time.ZoneId

case class AppConfig(
    raw: Config,
    timeZone: ZoneId,
    mongoConfig: MongoClient.Config,
    httpServerConfig: HttpServer.Config,
    dataInitializeConfig: DefaultDataRecordInitializer.Config,
)

object AppConfig {
  def load(resourceFile: String): AppConfig = {
    val config = ConfigFactory.load(resourceFile)
    AppConfig(
      raw = config,
      timeZone = ZoneId.of(config.getString("app.time-zone")),
      mongoConfig = MongoClient.Config.from(config, "app.mongo"),
      httpServerConfig = HttpServer.Config.from(config, "app.http"),
      dataInitializeConfig = DefaultDataRecordInitializer.Config(
        initializeRecordsOnStartup = config.getBoolean("app.initialize-records-on-startup"),
        initializeConfigOnStartup = config.getBoolean("app.initialize-config-on-startup")
      )
    )
  }
}
