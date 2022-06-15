package org.knoxmix.app

import com.typesafe.config.ConfigFactory

object Main extends App {

  val config = ConfigFactory.load()

  println("This is the docker config poc app (internal version)")
  println(config.getString("message"))

  Thread.sleep(3600000)

}
