package com.dzegel.DynamockServer.service

import com.dzegel.DynamockServer.types._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterEach, FunSuite, Matchers}

class ExpectationStoreTests extends FunSuite with MockFactory with Matchers with BeforeAndAfterEach {
  private var expectationStore: ExpectationStore = _

  override protected def beforeEach(): Unit = {
    expectationStore = new DefaultExpectationStore
  }

  test("registerExpectation works for paths") {
    val expectation1 = getExpectation(path = "path1")
    val request1 = getRequest(path = expectation1.path)
    val expectation2 = getExpectation(path = "path2")
    val request2 = getRequest(path = expectation2.path)

    testMultipleRegistrationsWork(expectation1, request1, expectation2, request2)
  }

  test("registerExpectation works for method") {
    val expectation1 = getExpectation(method = "method1")
    val request1 = getRequest(method = expectation1.method)
    val expectation2 = getExpectation(method = "method2")
    val request2 = getRequest(method = expectation2.method)

    testMultipleRegistrationsWork(expectation1, request1, expectation2, request2)
  }

  test("registerExpectation works for query params") {
    val expectation1 = getExpectation(queryParams = Set("key1" -> "value1"))
    val request1 = getRequest(queryParams = expectation1.queryParams)
    val expectation2 = getExpectation(queryParams = Set("key2" -> "value2", "key3" -> "value3"))
    val request2 = getRequest(queryParams = expectation2.queryParams)

    testMultipleRegistrationsWork(expectation1, request1, expectation2, request2)
  }

  test("registerExpectation works for overlapping query params") {
    val keyValue1 = "key1" -> "value1"
    val expectation1 = getExpectation(queryParams = Set(keyValue1))
    val request1 = getRequest(queryParams = expectation1.queryParams)
    val expectation2 = getExpectation(queryParams = Set(keyValue1, "key2" -> "value2"))
    val request2 = getRequest(queryParams = expectation2.queryParams)

    testMultipleRegistrationsWork(expectation1, request1, expectation2, request2)
  }

  test("registerExpectation works for stringContent") {
    val expectation1 = getExpectation(content = Content("stringContent1"))
    val request1 = getRequest(content = expectation1.content)
    val expectation2 = getExpectation(content = Content("stringContent2"))
    val request2 = getRequest(content = expectation2.content)

    testMultipleRegistrationsWork(expectation1, request1, expectation2, request2)
  }

  test("registerExpectation works for included header params") {
    val keyValue1 = "key1" -> "value1"
    val expectation1 = getExpectation(includedHeaders = Set(keyValue1))
    val id1 = expectation1.hashCode.toString
    val request1 = getRequest(headers = expectation1.headerParameters.included)
    val expectation2 = getExpectation(includedHeaders = Set(keyValue1, "key2" -> "value2"))
    val id2 = expectation2.hashCode.toString
    val request2 = getRequest(headers = expectation2.headerParameters.included)

    expectationStore.registerExpectation(expectation1) shouldBe id1
    expectationStore.getAllExpectations shouldBe Map(id1 -> expectation1)

    expectationStore.registerExpectation(expectation2) shouldBe id2
    expectationStore.getAllExpectations shouldBe Map(id1 -> expectation1, id2 -> expectation2)

    expectationStore.getIdsForMatchingExpectations(request1) shouldBe Set(id1)
    expectationStore.getMostConstrainedExpectationWithId(Set(id1)) should contain(id1 -> expectation1)

    expectationStore.getIdsForMatchingExpectations(request2) shouldBe Set(id1, id2)
    expectationStore.getMostConstrainedExpectationWithId(Set(id1, id2)) should contain(id2 -> expectation2)
  }

  test("registerExpectation works for excluded header params") {
    val keyValue1 = "key1" -> "value1"
    val expectation1 = getExpectation(excludedHeaders = Set(keyValue1))
    val expectation2 = getExpectation(excludedHeaders = Set(keyValue1, "key2" -> "value2"))
    val id1 = expectation1.hashCode.toString
    val id2 = expectation2.hashCode.toString
    val request1 = getRequest(headers = Set(keyValue1))
    val request2 = getRequest(headers = Set("key3" -> "value3"))
    val request3 = getRequest(headers = Set())

    expectationStore.registerExpectation(expectation1) shouldBe id1
    expectationStore.registerExpectation(expectation2) shouldBe id2

    expectationStore.getIdsForMatchingExpectations(request1) shouldBe empty

    expectationStore.getIdsForMatchingExpectations(request2) shouldBe Set(id1, id2)
    expectationStore.getMostConstrainedExpectationWithId(Set(id1, id2)) should contain(id2 -> expectation2)

    expectationStore.getIdsForMatchingExpectations(request3) shouldBe Set(id1, id2)
    expectationStore.getMostConstrainedExpectationWithId(Set(id1, id2)) should contain(id2 -> expectation2)
  }

  test("registerExpectation works for included and excluded header params") {
    val included1 = "includedKey1" -> "includedValue1"
    val excluded1 = "excludedKey1" -> "excludedValue1"
    val included2 = "includedKey2" -> "includedValue2"
    val excluded2 = "excludedKey2" -> "excludedValue2"
    val excluded3 = "excludedKey3" -> "excludedValue3"
    val expectation1 = getExpectation(includedHeaders = Set(included1), excludedHeaders = Set(excluded1, excluded2))
    val expectation2 = getExpectation(includedHeaders = Set(included1, included2), excludedHeaders = Set(excluded1))
    val expectation3 = getExpectation(includedHeaders = Set(included1, included2), excludedHeaders = Set(excluded1, excluded3))
    val id1 = expectation1.hashCode.toString
    val id2 = expectation2.hashCode.toString
    val id3 = expectation3.hashCode.toString

    expectationStore.registerExpectation(expectation1) shouldBe id1
    expectationStore.registerExpectation(expectation2) shouldBe id2
    expectationStore.registerExpectation(expectation3) shouldBe id3

    expectationStore.getIdsForMatchingExpectations(getRequest(headers = Set())) shouldBe empty
    expectationStore.getIdsForMatchingExpectations(getRequest(headers = Set())) shouldBe empty
    expectationStore.getIdsForMatchingExpectations(getRequest(headers = Set("Key" -> "Value"))) shouldBe empty
    expectationStore.getIdsForMatchingExpectations(getRequest(headers = Set(included1))) shouldBe Set(id1)
    expectationStore.getIdsForMatchingExpectations(getRequest(headers = Set(included2))) shouldBe empty
    expectationStore.getIdsForMatchingExpectations(getRequest(headers = Set(included1, included2))) shouldBe Set(id1, id2, id3)
    expectationStore.getIdsForMatchingExpectations(getRequest(headers = Set(included1, included2, excluded1))) shouldBe empty
    expectationStore.getIdsForMatchingExpectations(getRequest(headers = Set(included1, included2, "Key" -> "Value"))) shouldBe Set(id1, id2, id3)

    expectationStore.getIdsForMatchingExpectations(getRequest(headers = Set(included1, included2, excluded3))) shouldBe Set(id1, id2)
    expectationStore.getMostConstrainedExpectationWithId(Set(id1, id2)) should contain oneElementOf Seq(id1 -> expectation1, id2 -> expectation2)
  }

  test("getAllExpectations returns registered expectations") {
    val expectation = Expectation(
      "GET",
      "/some/path",
      Set("QueryParam" -> "QueryValue"),
      HeaderParameters(Set("IncludedHeader" -> "IncludedValue"), Set("ExcludedHeader" -> "ExcludedValue")),
      Content("Some Content"))
    val id = expectation.hashCode.toString

    expectationStore.getAllExpectations shouldBe empty

    expectationStore.registerExpectation(expectation) shouldBe id

    expectationStore.getAllExpectations shouldBe Map(id -> expectation)
  }

  test("clearAllExpectations clears all expectations") {
    val expectation1 = getExpectation(path = "path1", method = "GET")
    val id1 = expectation1.hashCode.toString
    val request1 = getRequest(path = expectation1.path, method = expectation1.method)
    val expectation2 = getExpectation(path = "path2", method = "PUT")
    val request2 = getRequest(path = expectation2.path, method = expectation2.method)
    val id2 = expectation2.hashCode.toString

    expectationStore.registerExpectation(expectation1) shouldBe id1
    expectationStore.getAllExpectations shouldBe Map(id1 -> expectation1)

    expectationStore.registerExpectation(expectation2) shouldBe id2
    expectationStore.getAllExpectations shouldBe Map(id1 -> expectation1, id2 -> expectation2)

    expectationStore.getIdsForMatchingExpectations(request1) shouldBe Set(id1)
    expectationStore.getMostConstrainedExpectationWithId(Set(id1)) should contain(id1 -> expectation1)

    expectationStore.getIdsForMatchingExpectations(request2) shouldBe Set(id2)
    expectationStore.getMostConstrainedExpectationWithId(Set(id2)) should contain(id2 -> expectation2)

    expectationStore.clearAllExpectations()

    expectationStore.getIdsForMatchingExpectations(request1) shouldBe empty
    expectationStore.getIdsForMatchingExpectations(request2) shouldBe empty

    expectationStore.getAllExpectations shouldBe empty
  }

  test("clearExpectations clears targeted expectations") {
    val expectation1 = getExpectation(path = "path1", method = "GET")
    val id1 = expectation1.hashCode.toString
    val request1 = getRequest(path = expectation1.path, method = expectation1.method)
    val expectation2 = getExpectation(path = "path2", method = "PUT")
    val id2 = expectation2.hashCode.toString
    val request2 = getRequest(path = expectation2.path, method = expectation2.method)

    expectationStore.registerExpectation(expectation1) shouldBe id1
    expectationStore.getAllExpectations shouldBe Map(id1 -> expectation1)

    expectationStore.registerExpectation(expectation2) shouldBe id2
    expectationStore.getAllExpectations shouldBe Map(id1 -> expectation1, id2 -> expectation2)

    expectationStore.getIdsForMatchingExpectations(request1) shouldBe Set(id1)
    expectationStore.getMostConstrainedExpectationWithId(Set(id1)) should contain(id1 -> expectation1)

    expectationStore.getIdsForMatchingExpectations(request2) should equal(Set(id2))
    expectationStore.getMostConstrainedExpectationWithId(Set(id2)) should contain(id2 -> expectation2)

    expectationStore.clearExpectations(Set(id1))

    expectationStore.getIdsForMatchingExpectations(request1) shouldBe empty
    expectationStore.getIdsForMatchingExpectations(request2) shouldBe Set(id2)

    expectationStore.getAllExpectations shouldBe Map(id2 -> expectation2)
  }

  private def testMultipleRegistrationsWork(expectation1: Expectation, request1: Request, expectation2: Expectation, request2: Request) {
    val id1 = expectation1.hashCode.toString
    val id2 = expectation2.hashCode.toString

    expectationStore.registerExpectation(expectation1) shouldBe id1
    expectationStore.getAllExpectations shouldBe Map(id1 -> expectation1)

    expectationStore.registerExpectation(expectation2) shouldBe id2
    expectationStore.getAllExpectations shouldBe Map(id1 -> expectation1, id2 -> expectation2)

    expectationStore.getIdsForMatchingExpectations(request1) shouldBe Set(id1)
    expectationStore.getMostConstrainedExpectationWithId(Set(id1)) should contain(id1 -> expectation1)

    expectationStore.getIdsForMatchingExpectations(request2) shouldBe Set(id2)
    expectationStore.getMostConstrainedExpectationWithId(Set(id2)) should contain(id2 -> expectation2)
  }

  private def getExpectation(
    path: Path = "",
    method: Method = "",
    queryParams: QueryParams = Set.empty,
    includedHeaders: HeaderSet = Set.empty,
    excludedHeaders: HeaderSet = Set.empty,
    content: Content = Content("")): Expectation = {
    Expectation(method, path, queryParams, HeaderParameters(includedHeaders, excludedHeaders), content)
  }

  private def getRequest(
    path: Path = "",
    method: Method = "",
    queryParams: QueryParams = Set.empty,
    headers: HeaderSet = Set.empty,
    content: Content = Content("")): Request = {
    Request(method, path, queryParams, headers, content)
  }
}
