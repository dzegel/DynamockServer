package com.dzegel.DynamockServer.service

import com.dzegel.DynamockServer.service.ExpectationStore.RegisterExpectationResponseReturnValue
import com.dzegel.DynamockServer.types._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterEach, FunSuite, Matchers}

class ExpectationStoreTests extends FunSuite with MockFactory with Matchers with BeforeAndAfterEach {
  private var expectationStore: ExpectationStore = _
  private var mockRandomStringGenerator: RandomStringGenerator = _

  private val response100 = Response(100, "", Map.empty)
  private val response200 = Response(200, "", Map.empty)
  private val response300 = Response(300, "", Map.empty)

  private val id1 = "id_1"
  private val id2 = "id_2"
  private val id3 = "id_3"

  override protected def beforeEach(): Unit = {
    mockRandomStringGenerator = mock[RandomStringGenerator]
    expectationStore = new DefaultExpectationStore(mockRandomStringGenerator)
  }

  test("registerExpectationResponse and getResponse works for paths") {
    val expectation1 = getExpectation(path = "path1")
    val request1 = getRequest(path = expectation1.path)
    val expectation2 = getExpectation(path = "path2")
    val request2 = getRequest(path = expectation2.path)

    testMultipleRegistrationsWork(expectation1, request1, expectation2, request2)
  }

  test("registerExpectationResponse and getResponse works for method") {
    val expectation1 = getExpectation(method = "method1")
    val request1 = getRequest(method = expectation1.method)
    val expectation2 = getExpectation(method = "method2")
    val request2 = getRequest(method = expectation2.method)

    testMultipleRegistrationsWork(expectation1, request1, expectation2, request2)
  }

  test("registerExpectationResponse and getResponse works for query params") {
    val expectation1 = getExpectation(queryParams = Map("key1" -> "value1"))
    val request1 = getRequest(queryParams = expectation1.queryParams)
    val expectation2 = getExpectation(queryParams = Map("key2" -> "value2", "key3" -> "value3"))
    val request2 = getRequest(queryParams = expectation2.queryParams)

    testMultipleRegistrationsWork(expectation1, request1, expectation2, request2)
  }

  test("registerExpectationResponse and getResponse works for overlapping query params") {
    val keyValue1 = "key1" -> "value1"
    val expectation1 = getExpectation(queryParams = Map(keyValue1))
    val request1 = getRequest(queryParams = expectation1.queryParams)
    val expectation2 = getExpectation(queryParams = Map(keyValue1, "key2" -> "value2"))
    val request2 = getRequest(queryParams = expectation2.queryParams)

    testMultipleRegistrationsWork(expectation1, request1, expectation2, request2)
  }

  test("registerExpectationResponse and getResponse works for stringContent") {
    val expectation1 = getExpectation(content = Content("stringContent1"))
    val request1 = getRequest(content = expectation1.content)
    val expectation2 = getExpectation(content = Content("stringContent2"))
    val request2 = getRequest(content = expectation2.content)

    testMultipleRegistrationsWork(expectation1, request1, expectation2, request2)
  }

  test("registerExpectationResponse and getResponse works for included header params") {
    val keyValue1 = "key1" -> "value1"
    val expectation1 = getExpectation(includedHeaders = Set(keyValue1))
    val request1 = getRequest(headers = expectation1.headerParameters.included)
    val expectation2 = getExpectation(includedHeaders = Set(keyValue1, "key2" -> "value2"))
    val request2 = getRequest(headers = expectation2.headerParameters.included)

    (mockRandomStringGenerator.next _).expects().returns(id1)
    val returnValue1 = expectationStore.registerExpectationResponse(expectation1, response100)
    returnValue1 shouldBe RegisterExpectationResponseReturnValue(id1, isResponseUpdated = false)
    val expectationResponseWithId1 = id1 -> (expectation1 -> response100)
    expectationStore.getAllExpectations should equal(Set(expectationResponseWithId1))

    (mockRandomStringGenerator.next _).expects().returns(id2)
    val returnValue2 = expectationStore.registerExpectationResponse(expectation2, response200)
    returnValue2 shouldBe RegisterExpectationResponseReturnValue(id2, isResponseUpdated = false)
    val expectationResponseWithId2 = id2 -> (expectation2 -> response200)
    expectationStore.getAllExpectations should equal(Set(expectationResponseWithId1, expectationResponseWithId2))

    expectationStore.getIdsForMatchingExpectations(request1) should equal(Set(id1))
    expectationStore.getMostConstrainedExpectationWithId(Set(id1)) should contain(expectationResponseWithId1)

    expectationStore.getIdsForMatchingExpectations(request2) shouldBe Set(id1, id2)
    expectationStore.getMostConstrainedExpectationWithId(Set(id1, id2)) should contain(expectationResponseWithId2)

    expectationStore.getIdsForMatchingExpectations(request1) should equal(Set(id1))
    expectationStore.getMostConstrainedExpectationWithId(Set(id1)) should contain(expectationResponseWithId1)

    val returnValue3 = expectationStore.registerExpectationResponse(expectation1, response300)
    returnValue3 shouldBe RegisterExpectationResponseReturnValue(id1, isResponseUpdated = true)
    val expectationResponseWithId3 = id1 -> (expectation1 -> response300)
    expectationStore.getAllExpectations should equal(Set(expectationResponseWithId2, expectationResponseWithId3))

    expectationStore.getIdsForMatchingExpectations(request1) should equal(Set(id1))
    expectationStore.getMostConstrainedExpectationWithId(Set(id1)) should contain(expectationResponseWithId3)

    expectationStore.getIdsForMatchingExpectations(request2) shouldBe Set(id1, id2)
    expectationStore.getMostConstrainedExpectationWithId(Set(id1, id2)) should contain(expectationResponseWithId2)
  }

  test("registerExpectationResponse and getResponse works for excluded header params") {
    val keyValue1 = "key1" -> "value1"
    val expectation1 = getExpectation(excludedHeaders = Set(keyValue1))
    val expectation2 = getExpectation(excludedHeaders = Set(keyValue1, "key2" -> "value2"))
    val request1 = getRequest(headers = Set(keyValue1))
    val request2 = getRequest(headers = Set("key3" -> "value3"))
    val request3 = getRequest(headers = Set())

    (mockRandomStringGenerator.next _).expects().returns(id1)
    val returnValue1 = expectationStore.registerExpectationResponse(expectation1, response100)
    returnValue1 shouldBe RegisterExpectationResponseReturnValue(id1, isResponseUpdated = false)

    (mockRandomStringGenerator.next _).expects().returns(id2)
    val returnValue2 = expectationStore.registerExpectationResponse(expectation2, response200)
    returnValue2 shouldBe RegisterExpectationResponseReturnValue(id2, isResponseUpdated = false)

    expectationStore.getIdsForMatchingExpectations(request1) shouldBe empty

    val matchingIds1 = expectationStore.getIdsForMatchingExpectations(request2)
    matchingIds1 shouldBe Set(returnValue1.expectationId, returnValue2.expectationId)
    expectationStore.getMostConstrainedExpectationWithId(matchingIds1) should contain(returnValue2.expectationId -> (expectation2, response200))

    val matchingIds2 = expectationStore.getIdsForMatchingExpectations(request3)
    matchingIds2 shouldBe Set(returnValue1.expectationId, returnValue2.expectationId)
    expectationStore.getMostConstrainedExpectationWithId(matchingIds2) should contain(returnValue2.expectationId -> (expectation2, response200))

    val returnValue12 = expectationStore.registerExpectationResponse(expectation1, response200)
    returnValue12 shouldBe RegisterExpectationResponseReturnValue(returnValue1.expectationId, isResponseUpdated = true)
    val returnValue21 = expectationStore.registerExpectationResponse(expectation2, response100)
    returnValue21 shouldBe RegisterExpectationResponseReturnValue(returnValue2.expectationId, isResponseUpdated = true)

    expectationStore.getIdsForMatchingExpectations(request1) shouldBe empty

    val matchingIds3 = expectationStore.getIdsForMatchingExpectations(request3)
    matchingIds3 shouldBe Set(returnValue1.expectationId, returnValue2.expectationId)
    expectationStore.getMostConstrainedExpectationWithId(matchingIds3) should contain(returnValue2.expectationId -> (expectation2, response100))

    val matchingIds4 = expectationStore.getIdsForMatchingExpectations(request3)
    matchingIds4 shouldBe Set(returnValue1.expectationId, returnValue2.expectationId)
    expectationStore.getMostConstrainedExpectationWithId(matchingIds4) should contain(returnValue2.expectationId -> (expectation2, response100))
  }

  test("registerExpectationResponse and getResponse works for included and excluded header params") {
    val included1 = "includedKey1" -> "includedValue1"
    val excluded1 = "excludedKey1" -> "excludedValue1"
    val included2 = "includedKey2" -> "includedValue2"
    val excluded2 = "excludedKey2" -> "excludedValue2"
    val excluded3 = "excludedKey3" -> "excludedValue3"
    val expectation1 = getExpectation(includedHeaders = Set(included1), excludedHeaders = Set(excluded1, excluded2))
    val expectation2 = getExpectation(includedHeaders = Set(included1, included2), excludedHeaders = Set(excluded1))
    val expectation3 = getExpectation(includedHeaders = Set(included1, included2), excludedHeaders = Set(excluded1, excluded3))

    (mockRandomStringGenerator.next _).expects().returns(id1)
    val returnValue1 = expectationStore.registerExpectationResponse(expectation1, response100)
    returnValue1 shouldBe RegisterExpectationResponseReturnValue(id1, isResponseUpdated = false)

    (mockRandomStringGenerator.next _).expects().returns(id2)
    val returnValue2 = expectationStore.registerExpectationResponse(expectation2, response200)
    returnValue2 shouldBe RegisterExpectationResponseReturnValue(id2, isResponseUpdated = false)

    (mockRandomStringGenerator.next _).expects().returns(id3)
    val returnValue3 = expectationStore.registerExpectationResponse(expectation3, response300)
    returnValue3 shouldBe RegisterExpectationResponseReturnValue(id3, isResponseUpdated = false)

    expectationStore.getIdsForMatchingExpectations(getRequest(headers = Set())) shouldBe empty
    expectationStore.getIdsForMatchingExpectations(getRequest(headers = Set())) shouldBe empty
    expectationStore.getIdsForMatchingExpectations(getRequest(headers = Set("Key" -> "Value"))) shouldBe empty
    expectationStore.getIdsForMatchingExpectations(getRequest(headers = Set(included1))) shouldBe Set(id1)
    expectationStore.getIdsForMatchingExpectations(getRequest(headers = Set(included2))) shouldBe empty
    expectationStore.getIdsForMatchingExpectations(getRequest(headers = Set(included1, included2))) shouldBe Set(id1, id2, id3)
    expectationStore.getIdsForMatchingExpectations(getRequest(headers = Set(included1, included2, excluded1))) shouldBe empty
    expectationStore.getIdsForMatchingExpectations(getRequest(headers = Set(included1, included2, "Key" -> "Value"))) shouldBe Set(id1, id2, id3)

    val idsFromMultipleValidExpectations = expectationStore.getIdsForMatchingExpectations(getRequest(headers = Set(included1, included2, excluded3)))
    idsFromMultipleValidExpectations shouldBe Set(id1, id2)
    val responseFromMultipleValidExpectations = expectationStore.getMostConstrainedExpectationWithId(idsFromMultipleValidExpectations)
    responseFromMultipleValidExpectations should contain oneElementOf Seq(id1 -> (expectation1, response100), id2 -> (expectation2, response200))

    //reversing these expectation -> response setups
    val returnValue12 = expectationStore.registerExpectationResponse(expectation1, response200)
    returnValue12 shouldBe RegisterExpectationResponseReturnValue(id1, isResponseUpdated = true)
    val returnValue21 = expectationStore.registerExpectationResponse(expectation2, response100)
    returnValue21 shouldBe RegisterExpectationResponseReturnValue(id2, isResponseUpdated = true)

    //the setup should be deterministic based on the expectations so making the same request after reversing the setup should result in a reversed response
    val idsFromMultipleValidReversedExpectations = expectationStore.getIdsForMatchingExpectations(getRequest(headers = Set(included1, included2, excluded3)))
    idsFromMultipleValidReversedExpectations shouldBe Set(id1, id2)
    val responseFromMultipleValidReversedExpectations = expectationStore.getMostConstrainedExpectationWithId(idsFromMultipleValidReversedExpectations)
    responseFromMultipleValidReversedExpectations should contain oneElementOf Seq(id1 -> (expectation1, response200), id2 -> (expectation2, response100))
  }

  test("getAllExpectations returns registered expectations and responses") {
    val expectation = Expectation(
      "GET",
      "/some/path",
      Map("QueryParam" -> "QueryValue"),
      HeaderParameters(Set("IncludedHeader" -> "IncludedValue"), Set("ExcludedHeader" -> "ExcludedValue")),
      Content("Some Content"))
    val response = Response(200, "Response Content", Map("ResponseHeaderParam" -> "ResponseHeaderValue"))

    expectationStore.getAllExpectations shouldBe empty

    (mockRandomStringGenerator.next _).expects().returns(id1)
    val returnValue = expectationStore.registerExpectationResponse(expectation, response)
    returnValue shouldBe RegisterExpectationResponseReturnValue(id1, isResponseUpdated = false)

    expectationStore.getAllExpectations shouldBe Set(returnValue.expectationId -> (expectation -> response))
  }

  test("clearAllExpectations clears all expectations") {
    val expectation1 = getExpectation(path = "path1", method = "GET")
    val request1 = getRequest(path = expectation1.path, method = expectation1.method)
    val expectation2 = getExpectation(path = "path2", method = "PUT")
    val request2 = getRequest(path = expectation2.path, method = expectation2.method)

    (mockRandomStringGenerator.next _).expects().returns(id1)
    val returnValue1 = expectationStore.registerExpectationResponse(expectation1, response100)
    returnValue1 shouldBe RegisterExpectationResponseReturnValue(id1, isResponseUpdated = false)
    val expectationResponseWithId1 = id1 -> (expectation1 -> response100)
    expectationStore.getAllExpectations should equal(Set(expectationResponseWithId1))

    (mockRandomStringGenerator.next _).expects().returns(id2)
    val returnValue2 = expectationStore.registerExpectationResponse(expectation2, response200)
    returnValue2 shouldBe RegisterExpectationResponseReturnValue(id2, isResponseUpdated = false)
    val expectationResponseWithId2 = id2 -> (expectation2 -> response200)
    expectationStore.getAllExpectations should equal(Set(expectationResponseWithId1, expectationResponseWithId2))

    expectationStore.getIdsForMatchingExpectations(request1) should equal(Set(id1))
    expectationStore.getMostConstrainedExpectationWithId(Set(id1)) should contain(expectationResponseWithId1)

    expectationStore.getIdsForMatchingExpectations(request2) should equal(Set(id2))
    expectationStore.getMostConstrainedExpectationWithId(Set(id2)) should contain(expectationResponseWithId2)

    expectationStore.clearAllExpectations()

    expectationStore.getIdsForMatchingExpectations(request1) shouldBe empty
    expectationStore.getIdsForMatchingExpectations(request2) shouldBe empty

    expectationStore.getAllExpectations shouldBe empty
  }

  test("clearExpectations clears targeted expectations") {
    val expectation1 = getExpectation(path = "path1", method = "GET")
    val request1 = getRequest(path = expectation1.path, method = expectation1.method)
    val expectation2 = getExpectation(path = "path2", method = "PUT")
    val request2 = getRequest(path = expectation2.path, method = expectation2.method)

    (mockRandomStringGenerator.next _).expects().returns(id1)
    val returnValue1 = expectationStore.registerExpectationResponse(expectation1, response100)
    returnValue1 shouldBe RegisterExpectationResponseReturnValue(id1, isResponseUpdated = false)
    val expectationResponseWithId1 = id1 -> (expectation1 -> response100)
    expectationStore.getAllExpectations should equal(Set(expectationResponseWithId1))

    (mockRandomStringGenerator.next _).expects().returns(id2)
    val returnValue2 = expectationStore.registerExpectationResponse(expectation2, response200)
    returnValue2 shouldBe RegisterExpectationResponseReturnValue(id2, isResponseUpdated = false)
    val expectationResponseWithId2 = id2 -> (expectation2 -> response200)
    expectationStore.getAllExpectations should equal(Set(expectationResponseWithId1, expectationResponseWithId2))

    expectationStore.getIdsForMatchingExpectations(request1) should equal(Set(id1))
    expectationStore.getMostConstrainedExpectationWithId(Set(id1)) should contain(expectationResponseWithId1)

    expectationStore.getIdsForMatchingExpectations(request2) should equal(Set(id2))
    expectationStore.getMostConstrainedExpectationWithId(Set(id2)) should contain(expectationResponseWithId2)

    expectationStore.clearExpectations(Set(id1))

    expectationStore.getIdsForMatchingExpectations(request1) shouldBe empty
    expectationStore.getIdsForMatchingExpectations(request2) shouldBe Set(id2)

    expectationStore.getAllExpectations shouldBe Set(expectationResponseWithId2)
  }

  test("registerExpectationResponseWithId adds/overrides expectation with provided id") {
    val expectation = getExpectation(path = "path1", method = "GET", queryParams = Map("param1" -> "value2"), content = Content("Some Content"))

    expectationStore.getAllExpectations shouldBe empty

    expectationStore.registerExpectationResponseWithId(expectation -> response100, id1)

    expectationStore.getAllExpectations shouldBe Set(id1 -> (expectation -> response100))

    expectationStore.registerExpectationResponseWithId(expectation -> response200, id2)

    expectationStore.getAllExpectations shouldBe Set(id2 -> (expectation -> response200))
  }

  private def testMultipleRegistrationsWork(expectation1: Expectation, request1: Request, expectation2: Expectation, request2: Request) {
    (mockRandomStringGenerator.next _).expects().returns(id1)
    val returnValue1 = expectationStore.registerExpectationResponse(expectation1, response100)
    returnValue1 shouldBe RegisterExpectationResponseReturnValue(id1, isResponseUpdated = false)
    val expectationResponseWithId1 = id1 -> (expectation1 -> response100)
    expectationStore.getAllExpectations should equal(Set(expectationResponseWithId1))

    (mockRandomStringGenerator.next _).expects().returns(id2)
    val returnValue2 = expectationStore.registerExpectationResponse(expectation2, response200)
    returnValue2 shouldBe RegisterExpectationResponseReturnValue(id2, isResponseUpdated = false)
    val expectationResponseWithId2 = id2 -> (expectation2 -> response200)
    expectationStore.getAllExpectations should equal(Set(expectationResponseWithId1, expectationResponseWithId2))

    expectationStore.getIdsForMatchingExpectations(request1) should equal(Set(id1))
    expectationStore.getMostConstrainedExpectationWithId(Set(id1)) should contain(expectationResponseWithId1)

    expectationStore.getIdsForMatchingExpectations(request2) shouldBe Set(id2)
    expectationStore.getMostConstrainedExpectationWithId(Set(id2)) should contain(expectationResponseWithId2)

    expectationStore.getIdsForMatchingExpectations(request1) should equal(Set(id1))
    expectationStore.getMostConstrainedExpectationWithId(Set(id1)) should contain(expectationResponseWithId1)

    val returnValue3 = expectationStore.registerExpectationResponse(expectation1, response300)
    returnValue3 shouldBe RegisterExpectationResponseReturnValue(id1, isResponseUpdated = true)
    val expectationResponseWithId3 = id1 -> (expectation1 -> response300)
    expectationStore.getAllExpectations should equal(Set(expectationResponseWithId2, expectationResponseWithId3))

    expectationStore.getIdsForMatchingExpectations(request1) should equal(Set(id1))
    expectationStore.getMostConstrainedExpectationWithId(Set(id1)) should contain(expectationResponseWithId3)

    expectationStore.getIdsForMatchingExpectations(request2) shouldBe Set(id2)
    expectationStore.getMostConstrainedExpectationWithId(Set(id2)) should contain(expectationResponseWithId2)
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
