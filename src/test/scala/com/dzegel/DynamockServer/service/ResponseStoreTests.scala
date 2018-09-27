package com.dzegel.DynamockServer.service

import com.dzegel.DynamockServer.types.Response
import org.scalatest.{BeforeAndAfterEach, FunSuite, Matchers}

class ResponseStoreTests extends FunSuite with BeforeAndAfterEach with Matchers {
  private var responseStore: ResponseStore = _

  private val expectationId1 = "1"
  private val expectationId2 = "2"
  private val expectationIds = Set(expectationId1, expectationId2)
  private val response1 = Response(100, "content 1", Map("1" -> "1"))
  private val response2 = Response(200, "content 2", Map("2" -> "2"))
  private val response3 = Response(300, "content 3", Map("3" -> "3"))

  override protected def beforeEach(): Unit = {
    responseStore = new DefaultResponseStore()
  }

  test("registerResponses and getResponses work") {
    val registerResult = responseStore.registerResponses(Map(expectationId1 -> response1, expectationId2 -> response2))
    val getResult = responseStore.getResponses(expectationIds)

    registerResult shouldBe Map(expectationId1 -> false, expectationId2 -> false)
    getResult shouldBe Map(expectationId1 -> response1, expectationId2 -> response2)
  }

  test("registerResponses and getResponses work for overwritten response") {
    val registerResult1 = responseStore.registerResponses(Map(expectationId1 -> response1, expectationId2 -> response2))
    val getResult1 = responseStore.getResponses(expectationIds)
    val registerResult2 = responseStore.registerResponses(Map(expectationId1 -> response1, expectationId2 -> response3))
    val getResult2 = responseStore.getResponses(expectationIds)

    registerResult1 shouldBe Map(expectationId1 -> false, expectationId2 -> false)
    getResult1 shouldBe Map(expectationId1 -> response1, expectationId2 -> response2)
    registerResult2 shouldBe Map(expectationId1 -> false, expectationId2 -> true)
    getResult2 shouldBe Map(expectationId1 -> response1, expectationId2 -> response3)
  }

  test("getResponses safely handles non-registered expectationIds") {
    val getResult1 = responseStore.getResponses(expectationIds)
    responseStore.registerResponses(Map(expectationId1 -> response1))
    val getResult2 = responseStore.getResponses(expectationIds)

    getResult1 shouldBe empty
    getResult2 shouldBe Map(expectationId1 -> response1)
  }

  test("deleteResponses") {
    val expectationIdToResponse = Map(expectationId1 -> response1, expectationId2 -> response2)

    responseStore.getResponses(expectationIds) shouldBe empty

    responseStore.registerResponses(expectationIdToResponse)
    responseStore.getResponses(expectationIds) shouldBe expectationIdToResponse
    responseStore.deleteResponses(expectationIds)
    responseStore.getResponses(expectationIds) shouldBe empty

    responseStore.registerResponses(expectationIdToResponse)
    responseStore.getResponses(expectationIds) shouldBe expectationIdToResponse
    responseStore.deleteResponses(Set(expectationId2))
    responseStore.getResponses(expectationIds) shouldBe Map(expectationId1 -> response1)
  }
}
