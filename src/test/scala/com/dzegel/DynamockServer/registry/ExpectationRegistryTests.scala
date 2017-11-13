package com.dzegel.DynamockServer.registry

import com.dzegel.DynamockServer.types._
import org.scalatest.{BeforeAndAfterEach, FunSuite, Matchers}

class ExpectationRegistryTests extends FunSuite with Matchers with BeforeAndAfterEach {
  private var expectationRegistry: ExpectationRegistry = _

  val response100 = Response(100, "", Map.empty)
  val response200 = Response(200, "", Map.empty)
  val response300 = Response(300, "", Map.empty)

  override protected def beforeEach(): Unit = {
    expectationRegistry = new DefaultExpectationRegistry()
  }

  test("registerExpectationWithResponse and getResponse works for paths") {
    val expectation1 = getExpectation(path = "path1")
    val request1 = getRequest(path = expectation1.path)
    val expectation2 = getExpectation(path = "path2")
    val request2 = getRequest(path = expectation2.path)

    testMultipleRegistrationsWork(expectation1, request1, expectation2, request2)
  }

  test("registerExpectationWithResponse and getResponse works for method") {
    val expectation1 = getExpectation(method = "method1")
    val request1 = getRequest(method = expectation1.method)
    val expectation2 = getExpectation(method = "method2")
    val request2 = getRequest(method = expectation2.method)

    testMultipleRegistrationsWork(expectation1, request1, expectation2, request2)
  }

  test("registerExpectationWithResponse and getResponse works for query params") {
    val expectation1 = getExpectation(queryParams = Map("key1" -> "value1"))
    val request1 = getRequest(queryParams = expectation1.queryParams)
    val expectation2 = getExpectation(queryParams = Map("key2" -> "value2", "key3" -> "value3"))
    val request2 = getRequest(queryParams = expectation2.queryParams)

    testMultipleRegistrationsWork(expectation1, request1, expectation2, request2)
  }

  test("registerExpectationWithResponse and getResponse works for overlapping query params") {
    val keyValue1 = "key1" -> "value1"
    val expectation1 = getExpectation(queryParams = Map(keyValue1))
    val request1 = getRequest(queryParams = expectation1.queryParams)
    val expectation2 = getExpectation(queryParams = Map(keyValue1, "key2" -> "value2"))
    val request2 = getRequest(queryParams = expectation2.queryParams)

    testMultipleRegistrationsWork(expectation1, request1, expectation2, request2)
  }

  test("registerExpectationWithResponse and getResponse works for stringContent") {
    val expectation1 = getExpectation(content = Content("stringContent1"))
    val request1 = getRequest(content = expectation1.content)
    val expectation2 = getExpectation(content = Content("stringContent2"))
    val request2 = getRequest(content = expectation2.content)

    testMultipleRegistrationsWork(expectation1, request1, expectation2, request2)
  }

  test("registerExpectationWithResponse and getResponse works for included header params") {
    val keyValue1 = "key1" -> "value1"
    val expectation1 = getExpectation(includedHeaders = Set(keyValue1))
    val request1 = getRequest(headers = expectation1.headerParameters.included)
    val expectation2 = getExpectation(includedHeaders = Set(keyValue1, "key2" -> "value2"))
    val request2 = getRequest(headers = expectation2.headerParameters.included)

    testMultipleRegistrationsWork(expectation1, request1, expectation2, request2)
  }

  test("registerExpectationWithResponse and getResponse works for excluded header params") {
    val keyValue1 = "key1" -> "value1"
    val expectation1 = getExpectation(excludedHeaders = Set(keyValue1))
    val expectation2 = getExpectation(excludedHeaders = Set(keyValue1, "key2" -> "value2"))
    val request1 = getRequest(headers = Set(keyValue1))
    val request2 = getRequest(headers = Set("key3" -> "value3"))
    val request3 = getRequest(headers = Set())

    expectationRegistry.registerExpectationWithResponse(expectation1, response100)
    expectationRegistry.registerExpectationWithResponse(expectation2, response200)

    expectationRegistry.getResponse(request1) shouldBe None
    expectationRegistry.getResponse(request2) should contain(response200)
    expectationRegistry.getResponse(request3) should contain(response200)

    expectationRegistry.registerExpectationWithResponse(expectation1, response200)
    expectationRegistry.registerExpectationWithResponse(expectation2, response100)

    expectationRegistry.getResponse(request1) shouldBe None
    expectationRegistry.getResponse(request2) should contain(response100)
    expectationRegistry.getResponse(request3) should contain(response100)
  }

  test("registerExpectationWithResponse and getResponse works for included and excluded header params") {
    val included1 = "includedKey1" -> "includedValue1"
    val excluded1 = "excludedKey1" -> "excludedValue1"
    val included2 = "includedKey2" -> "includedValue2"
    val excluded2 = "excludedKey2" -> "excludedValue2"
    val excluded3 = "excludedKey3" -> "excludedValue3"
    val expectation1 = getExpectation(includedHeaders = Set(included1), excludedHeaders = Set(excluded1, excluded2))
    val expectation2 = getExpectation(includedHeaders = Set(included1, included2), excludedHeaders = Set(excluded1))
    val expectation3 = getExpectation(includedHeaders = Set(included1, included2), excludedHeaders = Set(excluded1, excluded3))

    expectationRegistry.registerExpectationWithResponse(expectation1, response100)
    expectationRegistry.registerExpectationWithResponse(expectation2, response200)
    expectationRegistry.registerExpectationWithResponse(expectation3, response300)

    expectationRegistry.getResponse(getRequest(headers = Set())) shouldBe None
    expectationRegistry.getResponse(getRequest(headers = Set("Key" -> "Value"))) shouldBe None
    expectationRegistry.getResponse(getRequest(headers = Set(included1))) should contain(response100)
    expectationRegistry.getResponse(getRequest(headers = Set(included2))) shouldBe None
    expectationRegistry.getResponse(getRequest(headers = Set(included1, included2))) should contain(response300)
    expectationRegistry.getResponse(getRequest(headers = Set(included1, included2, excluded1))) shouldBe None
    expectationRegistry.getResponse(getRequest(headers = Set(included1, included2, "Key" -> "Value"))) should contain(response300)

    val responseFromMultipleValidExpectations = expectationRegistry.getResponse(getRequest(headers = Set(included1, included2, excluded3)))
    responseFromMultipleValidExpectations should contain oneElementOf Seq(response100, response200)

    //reversing these expectation -> response setups
    expectationRegistry.registerExpectationWithResponse(expectation1, response200)
    expectationRegistry.registerExpectationWithResponse(expectation2, response100)

    //the setup should be deterministic based on the expectations so making the same request after reversing the setup should result in a reversed response
    val responseFromMultipleValidReversedExpectations = expectationRegistry.getResponse(getRequest(headers = Set(included1, included2, excluded3)))
    responseFromMultipleValidReversedExpectations should contain oneElementOf Seq(response100, response200)
    responseFromMultipleValidReversedExpectations shouldNot be(responseFromMultipleValidExpectations)
  }

  test("getAllExpectations returns registered expectations and responses") {
    val expectation = Expectation(
      "GET",
      "/some/path",
      Map("QueryParam" -> "QueryValue"),
      HeaderParameters(Set("IncludedHeader" -> "IncludedValue"), Set("ExcludedHeader" -> "ExcludedValue")),
      Content("Some Content"))
    val response = Response(200, "Response Content", Map("ResponseHeaderParam" -> "ResponseHeaderValue"))

    expectationRegistry.getAllExpectations shouldBe empty

    expectationRegistry.registerExpectationWithResponse(expectation, response)

    expectationRegistry.getAllExpectations shouldBe Set(expectation -> response)
  }

  test("clearAllExpectations clears all expectations") {
    val expectation1 = getExpectation(path = "path1", method = "GET")
    val request1 = getRequest(path = expectation1.path, method = expectation1.method)
    val expectation2 = getExpectation(path = "path2", method = "PUT")
    val request2 = getRequest(path = expectation2.path, method = expectation2.method)

    expectationRegistry.registerExpectationWithResponse(expectation1, response100)
    expectationRegistry.registerExpectationWithResponse(expectation2, response200)

    expectationRegistry.getResponse(request1) should contain(response100)
    expectationRegistry.getResponse(request2) should contain(response200)

    expectationRegistry.clearAllExpectations()

    expectationRegistry.getResponse(request1) shouldBe empty
    expectationRegistry.getResponse(request2) shouldBe empty
  }

  private def testMultipleRegistrationsWork(expectation1: Expectation, request1: Request, expectation2: Expectation, request2: Request) {
    expectationRegistry.registerExpectationWithResponse(expectation1, response100)
    expectationRegistry.registerExpectationWithResponse(expectation2, response200)

    expectationRegistry.getResponse(request1) should contain(response100)
    expectationRegistry.getResponse(request2) should contain(response200)
    expectationRegistry.getResponse(request1) should contain(response100)

    expectationRegistry.registerExpectationWithResponse(expectation1, response300)

    expectationRegistry.getResponse(request1) shouldNot contain(response100)
    expectationRegistry.getResponse(request1) should contain(response300)
    expectationRegistry.getResponse(request2) should contain(response200)
  }

  private def getExpectation(
    path: Path = "",
    method: Method = "",
    queryParams: QueryParams = Map.empty,
    includedHeaders: HeaderSet = Set.empty,
    excludedHeaders: HeaderSet = Set.empty,
    content: Content = Content("")): Expectation = {
    Expectation(method, path, queryParams, HeaderParameters(includedHeaders, excludedHeaders), content)
  }

  private def getRequest(
    path: Path = "",
    method: Method = "",
    queryParams: QueryParams = Map.empty,
    headers: HeaderSet = Set.empty,
    content: Content = Content("")): Request = {
    Request(method, path, queryParams, headers, content)
  }
}
