package com.mp.domain.data_record.process_configuration

import akka.actor.ActorSystem
import com.typesafe.scalalogging.StrictLogging

import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class DataRecordProcessConfigService(implicit
    ec: ExecutionContext,
    system: ActorSystem,
    processConfigRepository: DataRecordProcessConfigRepository
) extends StrictLogging {
  private val processPriorityCache: AtomicReference[Option[DataRecordProcessConfig]] = new AtomicReference(None)

  def save(priorityConfig: DataRecordProcessConfig): Future[Unit] = {
    if (priorityConfig.hasPriorityConflict) {
      DataRecordProcessConfig.ValidationErrors.PriorityMultipleDefinition.throwException
    }

    if (priorityConfig.hasOverlappingRanges) {
      DataRecordProcessConfig.ValidationErrors.AmountRangeOverlaps.throwException
    }

    processConfigRepository.save(priorityConfig).flatMap(_ => refreshConfig())
  }

  def get(): Future[DataRecordProcessConfig] = {
    processPriorityCache.get() match {
      case Some(config) => Future.successful(config)
      case None         => refreshConfig().map(_ => processPriorityCache.get().get)
    }
  }

  def initializeCache(): Unit = {
    implicit val scheduler = system.scheduler
    akka.pattern
      .retry(
        () => refreshConfig(),
        attempts = 5,
        delay = 5.seconds
      )
      .onComplete {
        case Success(_) =>
          logger.info("DataRecordProcessConfig cache initialized")
        case Failure(ex) =>
          logger.error("DataRecordProcessConfig failed to initialize cache, assigned value = null", ex)
      }
  }

  private def refreshConfig(): Future[Unit] = {
    processConfigRepository.get().map {
      case Some(config) =>
        logger.info("DataRecordProcessConfig config refreshed with existing value")
        processPriorityCache.set(Some(config))

      case None =>
        logger.info("DataRecordProcessConfig config refreshed with empty value")
        processPriorityCache.set(Some(DataRecordProcessConfig.empty))
    }
  }
}
