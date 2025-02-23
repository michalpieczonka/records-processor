package com.mp.domain.shared.exception

case class DomainException private (errors: Seq[String], details: Option[String] = None)
    extends RuntimeException(s"errors=${errors.mkString(",")}${details.map(" " + _).getOrElse("")}") {

  def msg: String = getMessage
}

object DomainException {
  def apply(error: String): DomainException =
    new DomainException(Seq(error), None)

  def apply(error: String, details: String): DomainException =
    new DomainException(Seq(error), Some(details))

  def apply(errors: Seq[String]): DomainException =
    new DomainException(errors, None)

  def apply(errors: Seq[String], details: String): DomainException =
    new DomainException(errors, Some(details))

  def throwIfPresent(errors: Seq[String]): Unit =
    if (errors.nonEmpty) throw DomainException(errors)

  def throwIfPresent(errors: Seq[String], details: String): Unit =
    if (errors.nonEmpty) throw DomainException(errors, details)
}
