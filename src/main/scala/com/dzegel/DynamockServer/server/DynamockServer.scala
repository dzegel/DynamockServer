package com.dzegel.DynamockServer.server

import com.dzegel.DynamockServer.controller.{ExpectationsController, MockController}
import com.dzegel.DynamockServer.registry.RegistryValuesInjectionModule
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.routing.HttpRouter

class DynamockServer extends HttpServer {
  override protected lazy val disableAdminHttpServer = true
  override protected lazy val failfastOnFlagsNotParsed: Boolean = true

  override protected lazy val modules = Seq(RegistryValuesInjectionModule)

  override protected def configureHttp(router: HttpRouter): Unit = {
    router
      .add[ExpectationsController]
      .add[MockController]
  }
}
