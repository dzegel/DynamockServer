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

  override protected def beforeEach(): Unit = {
    responseStore = new DefaultResponseStore()
  }

  test("registerResponse and getResponses work") {
    responseStore.registerResponse(expectationId1, response1) shouldBe false
    responseStore.getResponses(expectationIds) shouldBe Map(expectationId1 -> response1)
    responseStore.registerResponse(expectationId2, response2) shouldBe false
    responseStore.getResponses(expectationIds) shouldBe Map(expectationId1 -> response1, expectationId2 -> response2)
  }

  test("registerResponse and getResponses work for overwritten response") {
    responseStore.getResponses(expectationIds) shouldBe empty
    responseStore.registerResponse(expectationId1, response1) shouldBe false
    responseStore.getResponses(expectationIds) shouldBe Map(expectationId1 -> response1)
    responseStore.registerResponse(expectationId1, response1) shouldBe false
    responseStore.getResponses(expectationIds) shouldBe Map(expectationId1 -> response1)
    responseStore.registerResponse(expectationId1, response2) shouldBe true
    responseStore.getResponses(expectationIds) shouldBe Map(expectationId1 -> response2)
  }

  test("getResponses safely handles non-registered expectationIds") {
    val getResult1 = responseStore.getResponses(expectationIds)
    responseStore.registerResponse(expectationId1, response1)
    val getResult2 = responseStore.getResponses(expectationIds)

    getResult1 shouldBe empty
    getResult2 shouldBe Map(expectationId1 -> response1)
  }

  test("deleteResponses") {
    val expectationIdToResponse = Map(expectationId1 -> response1, expectationId2 -> response2)

    responseStore.getResponses(expectationIds) shouldBe empty

    responseStore.registerResponse(expectationId1, response1)
    responseStore.registerResponse(expectationId2, response2)
    responseStore.getResponses(expectationIds) shouldBe expectationIdToResponse
    responseStore.deleteResponses(expectationIds)
    responseStore.getResponses(expectationIds) shouldBe empty

    responseStore.registerResponse(expectationId1, response1)
    responseStore.registerResponse(expectationId2, response2)
    responseStore.getResponses(expectationIds) shouldBe expectationIdToResponse
    responseStore.deleteResponses(Set(expectationId2))
    responseStore.getResponses(expectationIds) shouldBe Map(expectationId1 -> response1)
  }

  test("clearAllResponses") {
    responseStore.registerResponse(expectationId1, response1)
    responseStore.registerResponse(expectationId2, response2)
    responseStore.getResponses(expectationIds) shouldBe Map(expectationId1 -> response1, expectationId2 -> response2)

    responseStore.clearAllResponses()

    responseStore.getResponses(expectationIds) shouldBe empty
  }
}
