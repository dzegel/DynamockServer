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
    val portNumberRegex = """:(\d+)""".r
    val portRangeStart = 2
    val portRangeEnd = 65534
    portNumber match {
      case portNumberRegex(number) if portRangeStart <= number.toInt && number.toInt <= portRangeEnd => number.toInt.toString //strip leading 0s
      case _ => throw new Exception(s"Dynamock Initialization Error: 'http.port' flag must be a colon prefixed integer in the range [$portRangeStart, $portRangeEnd] (i.e. :8080).")
    }
  }
}
