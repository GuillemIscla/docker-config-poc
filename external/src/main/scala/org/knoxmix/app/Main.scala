package org.knoxmix.app

import com.typesafe.config.ConfigFactory

object Main extends App {

  val config = ConfigFactory.load()

  println("This is the docker config poc app (external version)")
  println(config.getString("message"))
  println(config.getString("messageFromBase"))

  Thread.sleep(3600000)

}
