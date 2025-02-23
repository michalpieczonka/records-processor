package com.mp.domain.shared

import com.mp.domain.shared.exception.DomainException
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ValueObjectTest extends AnyFunSuite with Matchers {

  test("Valid phone numbers should be accepted") {
    PhoneNumber("123456789").value shouldBe "123456789"
    PhoneNumber("48123456789").value shouldBe "48123456789"
  }

  test("Phone number with spaces should be trimmed and accepted") {
    PhoneNumber(" 123456789 ").value shouldBe "123456789"
  }

  test("Invalid phone numbers should throw DomainException") {
    intercept[DomainException] {
      PhoneNumber("abc123456")
    }.getMessage should include("Invalid phone number")

    intercept[DomainException] {
      PhoneNumber("123")
    }.getMessage should include("Invalid phone number")

    intercept[DomainException] {
      PhoneNumber("123456789123")
    }.getMessage should include("Invalid phone number")

    intercept[DomainException] {
      PhoneNumber("")
    }.getMessage should include("Invalid phone number")
  }

  test("Valid names should be accepted") {
    Name("John").value shouldBe "John"
    Name("John Doe").value shouldBe "John Doe"
    Name("Élise Dupont").value shouldBe "Élise Dupont"
  }

  test("Names with extra spaces should be trimmed and accepted") {
    Name("  John  ").value shouldBe "John"
    Name("  John  Doe  ").value shouldBe "John Doe"
  }

  test("Invalid names should throw DomainException") {
    intercept[DomainException] {
      Name("John123")
    }.getMessage should include("Invalid name")

    intercept[DomainException] {
      Name("John_Doe")
    }.getMessage should include("Invalid name")

    intercept[DomainException] {
      Name("123")
    }.getMessage should include("Invalid name")

    intercept[DomainException] {
      Name("")
    }.getMessage should include("Invalid name")
  }

  test("Valid amounts should be accepted") {
    Amount(100).value shouldBe 100
    Amount(0).value shouldBe 0
    Amount(9999.99).value shouldBe 9999.99
  }

  test("Negative amounts should throw DomainException") {
    intercept[DomainException] {
      Amount(-1)
    }.getMessage should include("Invalid amount")

    intercept[DomainException] {
      Amount(-100.50)
    }.getMessage should include("Invalid amount")
  }
}