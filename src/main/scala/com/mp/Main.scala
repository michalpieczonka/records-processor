package com.mp

object Main extends App {
  val config = AppConfig.load("app.conf")
  new Application(config).start()
}
