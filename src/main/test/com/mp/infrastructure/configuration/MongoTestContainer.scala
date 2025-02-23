package com.mp.infrastructure.configuration

import akka.actor.ActorSystem
import com.mp.infrastructure.configuration.mongo.MongoClient
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName

import scala.concurrent.ExecutionContext

object MongoTestContainer {
  private val container = new MongoDBContainer(DockerImageName.parse("mongo:5.0.13-focal"))

  def start(): Unit = container.start()
  def stop(): Unit = container.stop()

  def getMongoClient(implicit ec: ExecutionContext, system: ActorSystem) = {
    start()
    new MongoClient(MongoClient.Config(container.getConnectionString, "test-db"))
  }
}
