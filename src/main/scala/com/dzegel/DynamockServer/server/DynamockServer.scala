package com.dzegel.DynamockServer.server

import com.dzegel.DynamockServer.controller.{ExpectationController, MockController}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.routing.HttpRouter

class DynamockServer extends HttpServer {
  override protected lazy val defaultFinatraHttpPort: String = DynamockServer.tryGetPortFromArgs(args)

  override protected val disableAdminHttpServer = true

  override protected val allowUndefinedFlags: Boolean = true

  override protected def configureHttp(router: HttpRouter): Unit = {
    router
      .add[ExpectationController]
      .add[MockController]
  }
}

object DynamockServer {
  private def tryGetPortFromArgs(args: Array[String]): String = args.collect {
    case arg if arg.startsWith("port=") =>
      if (arg.matches("""port=([\d]{4})"""))
        arg.substring(5)
      else
        throw new Exception("Dynamock Initialization Error: Argument 'port' must be a 4 digit value.")
  } match {
    case Array() => ":8080"
    case Array(port) => ":" + port
    case _ => throw new Exception("Dynamock Initialization Error: At most one 'port' argument can be defined.")
  }
}
