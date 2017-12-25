package com.dzegel.DynamockServer.registry

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import com.twitter.inject.annotations.Flag

object RegistryValuesInjectionModule extends TwitterModule {
  //http.port flag is built into finatra
  flag("expectations.path.base", "", "Url base path for the expectations controller.")

  @Singleton
  @Provides def fileRootRegistry(@Flag("http.port") port: String): FileRootRegistry =
    new DefaultFileRootRegistry(extractPortNumber(port))

  @Singleton
  @Provides def expectationsUrlPathBaseRegistry(@Flag("expectations.path.base") pathBase: String): ExpectationsUrlPathBaseRegistry =
    new DefaultExpectationsUrlPathBaseRegistry(pathBase)

  private def extractPortNumber(portNumber: String): String = {
    val portNumberRegex = """:([\d]{4})""".r
    portNumber match {
      case portNumberRegex(number) => number
      case _ => throw new Exception("Dynamock Initialization Error: 'http.port' flag must be a colon prefixed four digit port number (i.e. :8080).")
    }
  }
}
