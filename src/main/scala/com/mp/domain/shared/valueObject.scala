package com.mp.domain.shared

import com.mp.domain.shared.exception.DomainException

case class PhoneNumber private (value: String)
object PhoneNumber {
  def apply(value: String): PhoneNumber = {
    val cleanedValue = value.trim
    val phoneRegex = "^[0-9]{9,11}$".r

    if (phoneRegex.matches(cleanedValue)) new PhoneNumber(cleanedValue)
    else throw DomainException("PHONE_NUMBER_INVALID", s"Invalid phone number: $value")
  }
}
case class Name private (value: String)
object Name {
  def apply(value: String): Name = {
    val cleanedValue = value.trim.replaceAll("\\s+", " ")
    val nameRegex = "^[^0-9!@#$%^&*()_+=\\[\\]{};:'\"\\\\|,<.>/?]+$".r

    if (nameRegex.matches(cleanedValue)) new Name(cleanedValue)
    else throw DomainException("NAME_INVALID", s"Invalid name: $value")
  }
}

case class Amount private (value: BigDecimal)
object Amount {
  def apply(value: BigDecimal): Amount = {
    if (value >= 0) new Amount(value)
    else throw DomainException("AMOUNT_INVALID", s"Invalid amount: $value")
  }
}