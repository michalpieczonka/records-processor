package com.mp.domain.shared.id

class UuidFactory {
  def generate(): String = java.util.UUID.randomUUID().toString.replace("-", "")
}
