package com.dzegel.DynamockServer.server

import java.io.File

import com.dzegel.DynamockServer.controller.{ExpectationController, MockController}
import com.dzegel.DynamockServer.service.{ExpectationsUrlPathBaseRegistry, FileRootRegistry, PortNumberRegistry}
import com.google.inject.{Provides, Singleton}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.inject.TwitterModule

class DynamockServer extends HttpServer {
  private val portNumber = DynamockServer.tryGetArgValue(args, "port", "8080", arg =>
    if (arg.matches("""([\d]{4})""")) arg else throw new Exception("Dynamock Initialization Error: Argument 'port' must be a 4 digit value."))
  private val fileRoot = s"${File.listRoots.head.getCanonicalPath}${File.separator}Dynamock${File.separator}$portNumber"
  private val expectationsUrlPathBase = DynamockServer.tryGetArgValue(args, "expectations-url-path-base", "", arg => s"/$arg")
  new File(fileRoot).mkdirs()

  private val runTimeInjectionModule = new TwitterModule {
    @Singleton
    @Provides def portNumberRegistry: PortNumberRegistry = new PortNumberRegistry(portNumber)

    @Singleton
    @Provides def fileRootRegistry: FileRootRegistry = new FileRootRegistry(fileRoot)

    @Singleton
    @Provides def expectationsUrlPathBaseRegistry: ExpectationsUrlPathBaseRegistry = new ExpectationsUrlPathBaseRegistry(expectationsUrlPathBase)
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
  private def tryGetArgValue(args: Array[String], key: String, defaultValue: String, valueTransformation: String => String): String = {
    val argPrefix = s"$key="
    args.collect {
      case arg if arg.startsWith(argPrefix) => valueTransformation(arg.substring(argPrefix.length))
    } match {
      case Array() => defaultValue
      case Array(transformedArg) => transformedArg
      case _ => throw new Exception(s"Dynamock Initialization Error: At most one '$key' argument can be defined.")
    }
  }
}
