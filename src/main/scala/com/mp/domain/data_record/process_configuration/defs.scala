package com.mp.domain.data_record.process_configuration

import com.mp.domain.shared.exception.DomainException

case class DataRecordProcessConfig(
    prioritiesByAmount: Set[DataRecordProcessConfig.PriorityByAmountConfig]
) {
  def hasPriorityConflict: Boolean = {
    val groupedByPriority = prioritiesByAmount.groupBy(_.priority)
    groupedByPriority.exists { case (_, configs) => configs.size > 1 }
  }

  def hasOverlappingRanges: Boolean = {
    val sortedRanges = prioritiesByAmount.toSeq.sortBy(_.amountRange.from)
    sortedRanges.sliding(2).exists {
      case Seq(a, b) => a.amountRange.overlapsWith(b.amountRange)
      case _ => false
    }
  }
}
object DataRecordProcessConfig {
  def empty: DataRecordProcessConfig = DataRecordProcessConfig(Set.empty)

  case class Priority(value: Int)

  case class PriorityByAmountConfig(
      amountRange: DataRecordProcessConfig.AmountRange,
      priority: DataRecordProcessConfig.Priority
  )

  case class AmountRange(
      from: BigDecimal,
      to: Option[BigDecimal]
  ) {
    def overlapsWith(other: AmountRange): Boolean =
      from < other.to.getOrElse(BigDecimal(Int.MaxValue)) &&
        other.from < to.getOrElse(BigDecimal(Int.MaxValue))
  }

  sealed trait ValidationErrors {
    def code: String
    def message: String

    def throwException = throw DomainException(code, message)
  }
  object ValidationErrors {
    case object PriorityMultipleDefinition extends ValidationErrors {
      override val code    = "PRIORITY_MULTIPLE_DEFINITION"
      override val message = "Defined priorities duplicates each other"
    }
    case object AmountRangeOverlaps extends ValidationErrors {
      override val code    = "AMOUNT_RANGE_OVERLAPS"
      override val message = "Amount ranges overlaps each other"
    }
  }
}
