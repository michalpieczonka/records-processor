package com.mp.infrastructure.rest.data_record.process_configuration

object DataRecordProcessConfigApi {
  case class UpdateProcessConfigRequest(
      prioritiesByAmount: Seq[PriorityByAmountConfig]
  ) {
    def validateEntries(): Option[String] = {
      val sortedEntries = prioritiesByAmount.sortBy(_.from)

      val zeroFromCount = prioritiesByAmount.count(_.from == BigDecimal(0))
      val openEndedCount = prioritiesByAmount.count(_.to.isEmpty)

      if (zeroFromCount != 1)
        return Some("Missing entry where 'from' is zero")

      if (openEndedCount != 1)
        return Some("Missing entry where 'to' is undefined")

      sortedEntries.zipWithIndex.collectFirst { case (entry, idx) =>
        if (entry.from < 0) Some(s"Entry $idx: 'from' must be >= 0")
        else if (entry.to.exists(_ < 0)) Some(s"Entry $idx: 'to' must be >= 0 if defined")
        else if (entry.to.exists(_ <= entry.from)) Some(s"Entry $idx: 'from' must be less than 'to' if 'to' is defined")
        else if (entry.priority < 0) Some(s"Entry $idx: 'priority' must be >= 0")
        else None
      }.flatten.orElse {
        val conflicts = sortedEntries.sliding(2).collectFirst {
          case Seq(prev, next) if prev.to.contains(next.from) && next.to.contains(prev.from) =>
            Some(s"Invalid range overlap: '${prev.from} to ${prev.to.getOrElse("∞")}' conflicts with '${next.from} to ${next.to.getOrElse("∞")}'")
        }.flatten

        conflicts
      }
    }
  }

  case class GetProcessConfigResponse(
      prioritiesByAmount: Seq[PriorityByAmountConfig]
  )

  case class PriorityByAmountConfig(
      from: BigDecimal,
      to: Option[BigDecimal],
      priority: Int
  )
}
