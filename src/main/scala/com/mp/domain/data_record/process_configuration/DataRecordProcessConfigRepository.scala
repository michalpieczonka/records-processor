package com.mp.domain.data_record.process_configuration

import scala.concurrent.Future

trait DataRecordProcessConfigRepository {
  def save(processConfig: DataRecordProcessConfig): Future[Unit]
  def get(): Future[Option[DataRecordProcessConfig]]
}
