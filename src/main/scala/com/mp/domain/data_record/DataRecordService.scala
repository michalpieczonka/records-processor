package com.mp.domain.data_record

import com.mp.domain.data_record.process_configuration.DataRecordProcessConfigService
import com.mp.domain.shared.id.UuidFactory

import java.time.Clock
import java.time.temporal.ChronoUnit
import scala.concurrent.{ExecutionContext, Future}

class DataRecordService(implicit
    ec: ExecutionContext,
    clock: Clock,
    idFactory: UuidFactory,
    dataReportRepository: DataRecordRepository,
    dataRecordProcessConfigService: DataRecordProcessConfigService
) {
  def createRecord(command: DataRecordCommand.Create): Future[DataRecordId] = {
    val id = DataRecordId(idFactory.generate())

    val dataRecord = DataRecord(
      id = id,
      name = command.name,
      phoneNumber = command.phoneNumber,
      amount = command.amount,
      createTime = clock.instant(),
      processTime = None
    )

    dataReportRepository.save(dataRecord).map(_ => id)
  }

  def processRecord(): Future[Option[DataRecord]] = {
    val nowTime = clock.instant()
    for {
      processPrioritiesConfig <- dataRecordProcessConfigService.get()

      recordProcessCriteria = DataRecordProcessCriteria(
                                priorityByAmountConfig = processPrioritiesConfig.prioritiesByAmount,
                                unAllowedProcessEarliestTimeWithSamePhoneNumber = nowTime.minus(3, ChronoUnit.DAYS)
                              )

      processedRecord <- dataReportRepository.getNextRecordToProcess(recordProcessCriteria).flatMap {
                           case Some(record) =>
                             val processedRecord = record.setProcessed(nowTime)
                             dataReportRepository.save(processedRecord).map(_ => Some(processedRecord))

                           case None =>
                             Future.successful(None)
                         }

    } yield processedRecord
  }
}
