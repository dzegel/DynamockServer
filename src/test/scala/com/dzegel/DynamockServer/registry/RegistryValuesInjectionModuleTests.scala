package com.dzegel.DynamockServer.registry

import java.io.File

import org.scalatest.{FunSuite, Matchers}

class RegistryValuesInjectionModuleTests extends FunSuite with Matchers {

  test("""fileRootRegistry(":65534") works""") {
    RegistryValuesInjectionModule.fileRootRegistry(":65534").fileRoot shouldBe
      s"${File.listRoots.head.getCanonicalPath}${File.separator}DynamockServer${File.separator}65534"
  }

  test("""fileRootRegistry(":002") strips leading 0s""") {
    RegistryValuesInjectionModule.fileRootRegistry(":002").fileRoot shouldBe
      s"${File.listRoots.head.getCanonicalPath}${File.separator}DynamockServer${File.separator}2"
  }

  test("""fileRootRegistry(":1") throws""") {
    val thrown = intercept[Exception] {
      RegistryValuesInjectionModule.fileRootRegistry(":1")
    }
    thrown.getMessage shouldBe
      "Dynamock Initialization Error: 'http.port' flag must be a colon prefixed integer in the range [2, 65534] (i.e. :8080)."
  }

  test("""fileRootRegistry(":65535") throws""") {
    val thrown = intercept[Exception] {
      RegistryValuesInjectionModule.fileRootRegistry(":65535")
    }
    thrown.getMessage shouldBe
      "Dynamock Initialization Error: 'http.port' flag must be a colon prefixed integer in the range [2, 65534] (i.e. :8080)."
  }

  test("""dynamockUrlPathBaseRegistry("test") adds leading /""") {
    RegistryValuesInjectionModule.dynamockUrlPathBaseRegistry("test").pathBase shouldBe "/test"
  }

  test("""dynamockUrlPathBaseRegistry("/test") is a pass though""") {
    RegistryValuesInjectionModule.dynamockUrlPathBaseRegistry("/test").pathBase shouldBe "/test"
  }

  test("""dynamockUrlPathBaseRegistry("one/two") works""") {
    RegistryValuesInjectionModule.dynamockUrlPathBaseRegistry("one/two").pathBase shouldBe "/one/two"
  }
}
