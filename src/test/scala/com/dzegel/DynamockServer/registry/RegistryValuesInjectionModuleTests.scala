package com.dzegel.DynamockServer.registry

import java.io.File

import org.scalatest.{FunSuite, Matchers}

class RegistryValuesInjectionModuleTests extends FunSuite with Matchers {

  test("""fileRootRegistry(":80") works""") {
    RegistryValuesInjectionModule.fileRootRegistry(":80").fileRoot shouldBe
      s"${File.listRoots.head.getCanonicalPath}${File.separator}Dynamock${File.separator}80"
  }

  test("""fileRootRegistry(":0080") strips leading 0s""") {
    RegistryValuesInjectionModule.fileRootRegistry(":0080").fileRoot shouldBe
      s"${File.listRoots.head.getCanonicalPath}${File.separator}Dynamock${File.separator}80"
  }

  test("""expectationsUrlPathBaseRegistry("test") adds leading /""") {
    RegistryValuesInjectionModule.expectationsUrlPathBaseRegistry("test").pathBase shouldBe "/test"
  }

  test("""expectationsUrlPathBaseRegistry("/test") is a pass though""") {
    RegistryValuesInjectionModule.expectationsUrlPathBaseRegistry("/test").pathBase shouldBe "/test"
  }

  test("""expectationsUrlPathBaseRegistry("one/two") works""") {
    RegistryValuesInjectionModule.expectationsUrlPathBaseRegistry("one/two").pathBase shouldBe "/one/two"
  }
}
