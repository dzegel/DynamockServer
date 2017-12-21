package com.dzegel.DynamockServer.server

import java.io.File

import com.dzegel.DynamockServer.controller.{ExpectationController, MockController}
import com.dzegel.DynamockServer.service.{FileRootRegistry, PortNumberRegistry, ValueInjectionRegistry}
import com.google.inject.{Provides, Singleton}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.inject.TwitterModule

class DynamockServer extends HttpServer {
  private val portNumber = DynamockServer.tryGetPortFromArgs(args)
  private val fileRoot = s"${File.listRoots.head.getCanonicalPath}${File.separator}Dynamock${File.separator}$portNumber"
  new File(fileRoot).mkdirs()
  private val valueInjectionRegistry = new ValueInjectionRegistry(portNumber, fileRoot)

  private val runTimeInjectionModule = new TwitterModule {
    @Singleton
    @Provides def portNumberRegistry: PortNumberRegistry = valueInjectionRegistry

    @Singleton
    @Provides def fileRootRegistry: FileRootRegistry = valueInjectionRegistry
  }

  override val modules = Seq(runTimeInjectionModule)

  override protected lazy val defaultFinatraHttpPort: String = s":$portNumber"

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
    case Array() => "8080"
    case Array(port) => port
    case _ => throw new Exception("Dynamock Initialization Error: At most one 'port' argument can be defined.")
  }
}
