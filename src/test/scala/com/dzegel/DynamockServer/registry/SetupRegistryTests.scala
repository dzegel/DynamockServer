package com.dzegel.DynamockServer.registry

import com.dzegel.DynamockServer.contract.{Expectation, Response}
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

class SetupRegistryTests extends FunSuite with Matchers with BeforeAndAfterAll {
  private var setupRegistry: SetupRegistry = _

  override protected def beforeAll(): Unit = {
    setupRegistry = new DefaultSetupRegistry()
  }

  test("registerExpectationWithResponse and getResponse works for paths") {
    val expectation1 = getExpectation(path = "path1")
    val expectation2 = getExpectation(path = "path2")

    testMultipleRegistrationsWork(expectation1, expectation2)
  }

  test("registerExpectationWithResponse and getResponse works for method") {
    val expectation1 = getExpectation(method = "method1")
    val expectation2 = getExpectation(method = "method2")

    testMultipleRegistrationsWork(expectation1, expectation2)
  }

  test("registerExpectationWithResponse and getResponse works for stringContent") {
    val expectation1 = getExpectation(stringContent = "stringContent1")
    val expectation2 = getExpectation(stringContent = "stringContent2")

    testMultipleRegistrationsWork(expectation1, expectation2)
  }

  private def testMultipleRegistrationsWork(expectation1: Expectation, expectation2: Expectation) {

    val response1 = Response()
    val response2 = Response()
    val response3 = Response()

    setupRegistry.registerExpectationWithResponse(expectation1, response1)
    setupRegistry.registerExpectationWithResponse(expectation2, response2)

    setupRegistry.getResponse(expectation1).get eq response1 shouldBe true
    setupRegistry.getResponse(expectation2).get eq response2 shouldBe true
    setupRegistry.getResponse(expectation1).get eq response1 shouldBe true

    setupRegistry.registerExpectationWithResponse(expectation1, response3)

    setupRegistry.getResponse(expectation1).get ne response1 shouldBe true
    setupRegistry.getResponse(expectation1).get eq response3 shouldBe true
    setupRegistry.getResponse(expectation2).get eq response2 shouldBe true
  }

  private def getExpectation(path: Path = "", method: Method = "", stringContent: StringContent = ""): Expectation = {
    Expectation(path, method, stringContent)
  }
}
