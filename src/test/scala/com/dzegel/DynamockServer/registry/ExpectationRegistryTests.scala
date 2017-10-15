package com.dzegel.DynamockServer.registry

import com.dzegel.DynamockServer.types.{Content, Expectation, Response}
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

class ExpectationRegistryTests extends FunSuite with Matchers with BeforeAndAfterAll {
  private var expectationRegistry: ExpectationRegistry = _

  override protected def beforeAll(): Unit = {
    expectationRegistry = new DefaultExpectationRegistry()
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
    val expectation1 = getExpectation(content = Content("stringContent1"))
    val expectation2 = getExpectation(content = Content("stringContent2"))

    testMultipleRegistrationsWork(expectation1, expectation2)
  }

  private def testMultipleRegistrationsWork(expectation1: Expectation, expectation2: Expectation) {

    val response1 = Response(100, "", Map.empty)
    val response2 = Response(200, "", Map.empty)
    val response3 = Response(300, "", Map.empty)

    expectationRegistry.registerExpectationWithResponse(expectation1, response1)
    expectationRegistry.registerExpectationWithResponse(expectation2, response2)

    expectationRegistry.getResponse(expectation1) should contain(response1)
    expectationRegistry.getResponse(expectation2) should contain(response2)
    expectationRegistry.getResponse(expectation1) should contain(response1)

    expectationRegistry.registerExpectationWithResponse(expectation1, response3)

    expectationRegistry.getResponse(expectation1) shouldNot contain(response1)
    expectationRegistry.getResponse(expectation1) should contain(response3)
    expectationRegistry.getResponse(expectation2) should contain(response2)
  }

  private def getExpectation(path: Path = "", method: Method = "", content: Content = Content("")): Expectation = {
    Expectation(method, path, content)
  }
}
