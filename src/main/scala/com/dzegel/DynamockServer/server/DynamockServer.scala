package com.dzegel.DynamockServer.server

import com.dzegel.DynamockServer.controller.SetupController
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.routing.HttpRouter

object DynamockServerMain extends DynamockServer

class DynamockServer extends HttpServer {
  override val defaultFinatraHttpPort: String = ":8080"
  override val disableAdminHttpServer = true
  override protected def configureHttp(router: HttpRouter): Unit = {
    router
      .add[SetupController]
  }
}
