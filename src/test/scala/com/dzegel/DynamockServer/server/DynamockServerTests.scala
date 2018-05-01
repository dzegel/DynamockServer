package com.dzegel.DynamockServer.server

import com.dzegel.DynamockServer.types.{Content, Expectation, HeaderParameters, Response}
import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import org.json4s.JsonAST._
import org.json4s.native.JsonParser.parse
import org.scalatest.{BeforeAndAfterEach, Matchers}

class DynamockServerTests extends FeatureTest with Matchers with BeforeAndAfterEach {

  override protected val server: EmbeddedHttpServer = new EmbeddedHttpServer(
    new DynamockServer {
      override protected lazy val allowUndefinedFlags = true
    }, httpPortFlag = "anything", //setting this to anything other than 'http.port' and setting 'allowUndefinedFlags = true' is a hack that enables the test to pass in the http.port flag defined in args
    args = Seq("-http.port=:1235", "-dynamock.path.base=DynamockTest")
  )

  private val expectation = Expectation("PUT", "/some/path", Map.empty, HeaderParameters(Set.empty, Set.empty), Content("someContent"))
  private val response = Response(201, "SomeOtherContent", Map("SomeKey" -> "SomeValue"))
  private val response2 = Response(203, "SomeOtherContent2", Map("SomeKey2" -> "SomeValue2"))
  private val expectationName = "expectation name"

  private def expectationPutRequestJson(response: Response): String =
    s"""
{
  "expectation_responses": [{
    "expectation_name": "$expectationName",
    "expectation": {
      "path": "${expectation.path}",
      "method": "${expectation.method}",
      "content": "${expectation.content.stringValue}"
    },
    "response": {
      "status": ${response.status},
      "content": "${response.content}",
      "header_map": {
        "${response.headerMap.head._1}": "${response.headerMap.head._2}"
      }
    }
  }]
}"""

  override protected def beforeEach: Unit = server.httpDelete("/DynamockTest/expectations")

  test("PUT /DynamockTest/expectations - mocked expectation - DELETE /DynamockTest/expectations - mocked expectation returns the expected response") {
    val putExpectationsResponse = server.httpPut(
      path = "/DynamockTest/expectations",
      putBody = expectationPutRequestJson(response),
      andExpect = Status.Ok)

    val responseMap = parse(putExpectationsResponse.contentString).filterField(_ => true).toMap
    val expectationInfoJArray = responseMap("expectations_info")
    expectationInfoJArray shouldBe a[JArray]
    val expectationInfoSeq = expectationInfoJArray.values.asInstanceOf[::[Map[String, Any]]]
    expectationInfoSeq.size shouldBe 1
    val expectationInfoMap = expectationInfoSeq.head
    expectationInfoMap("expectation_name") shouldBe expectationName
    expectationInfoMap("did_overwrite_response") shouldBe false
    expectationInfoMap("expectation_id") shouldBe a[String]
    expectationInfoMap("expectation_id").asInstanceOf[String] should not be empty

    val mockResponse = server.httpPut(
      expectation.path,
      putBody = expectation.content.stringValue,
      andExpect = Status(response.status),
      withBody = response.content)

    mockResponse.headerMap should contain allElementsOf response.headerMap

    server.httpDelete(
      "/DynamockTest/expectations",
      andExpect = Status.NoContent)

    val mockResponse2 = server.httpPut(
      expectation.path,
      putBody = expectation.content.stringValue,
      andExpect = Status(551))

    val mockResponseMap = parse(mockResponse2.contentString).filterField(_ => true).toMap
    val jMessage = mockResponseMap("message")
    jMessage shouldBe JString("Dynamock Error: The request did not match any registered expectations.")
    val mockRequestMap = mockResponseMap("request").values.asInstanceOf[Map[String, Any]]
    mockRequestMap.keySet shouldBe Set("path", "method", "content", "headers", "query_params")
    mockRequestMap("path") shouldBe expectation.path
    mockRequestMap("method") shouldBe "PUT"
    mockRequestMap("content") shouldBe expectation.content.stringValue
    mockRequestMap("query_params").asInstanceOf[Map[String, Any]] shouldBe empty
  }

  test("PUT /DynamockTest/expectations - mocked expectation - PUT /DynamockTest/expectations with new response - mocked expectation returns the expected response") {
    val putExpectationsResponse = server.httpPut(
      path = "/DynamockTest/expectations",
      putBody = expectationPutRequestJson(response),
      andExpect = Status.Ok)

    val responseMap = parse(putExpectationsResponse.contentString).filterField(_ => true).toMap
    val expectationInfoJArray = responseMap("expectations_info")
    expectationInfoJArray shouldBe a[JArray]
    val expectationInfoSeq = expectationInfoJArray.values.asInstanceOf[::[Map[String, Any]]]
    expectationInfoSeq.size shouldBe 1
    val expectationInfoMap = expectationInfoSeq.head
    expectationInfoMap("expectation_name") shouldBe expectationName
    expectationInfoMap("did_overwrite_response") shouldBe false
    expectationInfoMap("expectation_id") shouldBe a[String]
    val expectationId = expectationInfoMap("expectation_id").asInstanceOf[String]
    expectationId should not be empty

    val mockResponse = server.httpPut(
      expectation.path,
      putBody = expectation.content.stringValue,
      andExpect = Status(response.status),
      withBody = response.content)

    mockResponse.headerMap should contain allElementsOf response.headerMap

    server.httpPut(
      path = "/DynamockTest/expectations",
      putBody = expectationPutRequestJson(response2),
      andExpect = Status.Ok,
      withJsonBody =
        s"""{
           |  "expectations_info": [{
           |    "expectation_name": "$expectationName",
           |    "expectation_id": "$expectationId",
           |    "did_overwrite_response": true
           |  }]
           |}""".stripMargin)

    val mockResponse2 = server.httpPut(
      expectation.path,
      putBody = expectation.content.stringValue,
      andExpect = Status(response2.status),
      withBody = response2.content)

    mockResponse2.headerMap should contain allElementsOf response2.headerMap
  }

  test("PUT - GET - DELETE - GET /DynamockTest/expectations returns the expected output") {
    val putExpectationsResponse = server.httpPut(
      path = "/DynamockTest/expectations",
      putBody = expectationPutRequestJson(response),
      andExpect = Status.Ok)

    val responseMap = parse(putExpectationsResponse.contentString).filterField(_ => true).toMap
    val expectationInfoJArray = responseMap("expectations_info")
    expectationInfoJArray shouldBe a[JArray]
    val expectationInfoSeq = expectationInfoJArray.values.asInstanceOf[::[Map[String, Any]]]
    expectationInfoSeq.size shouldBe 1
    val expectationInfoMap = expectationInfoSeq.head
    expectationInfoMap("expectation_name") shouldBe expectationName
    expectationInfoMap("did_overwrite_response") shouldBe false
    expectationInfoMap("expectation_id") shouldBe a[String]
    val expectationId = expectationInfoMap("expectation_id").asInstanceOf[String]
    expectationId should not be empty

    server.httpGet(
      "/DynamockTest/expectations",
      andExpect = Status.Ok,
      withJsonBody =
        s"""
{
  "expectation_responses": [{
    "expectation_id": "$expectationId",
    "expectation": {
      "path": "${expectation.path}",
      "method": "${expectation.method}",
      "content": "${expectation.content.stringValue}",
      "included_header_parameters": {},
      "excluded_header_parameters": {},
      "query_parameters": {}
    },
    "response": {
      "status": ${response.status},
      "content": "${response.content}",
      "header_map": {
        "${response.headerMap.head._1}": "${response.headerMap.head._2}"
      }
    }
  }]
}""")

    server.httpDelete(
      "/DynamockTest/expectations",
      andExpect = Status.NoContent)

    server.httpGet(
      "/DynamockTest/expectations",
      andExpect = Status.Ok,
      withJsonBody = """{ "expectation_responses": [] }""")
  }

  test("Expectations suite works") {
    val putExpectationsResponse = server.httpPut(
      path = "/DynamockTest/expectations",
      putBody = expectationPutRequestJson(response),
      andExpect = Status.Ok)

    val responseMap = parse(putExpectationsResponse.contentString).filterField(_ => true).toMap
    val expectationInfoJArray = responseMap("expectations_info")
    val expectationInfoSeq = expectationInfoJArray.values.asInstanceOf[::[Map[String, Any]]]
    val expectationInfoMap = expectationInfoSeq.head
    val expectationId = expectationInfoMap("expectation_id").asInstanceOf[String]

    server.httpGet(
      "/DynamockTest/expectations",
      andExpect = Status.Ok,
      withJsonBody =
        s"""
{
  "expectation_responses": [{
    "expectation_id": "$expectationId",
    "expectation": {
      "path": "${expectation.path}",
      "method": "${expectation.method}",
      "content": "${expectation.content.stringValue}",
      "included_header_parameters": {},
      "excluded_header_parameters": {},
      "query_parameters": {}
    },
    "response": {
      "status": ${response.status},
      "content": "${response.content}",
      "header_map": {
        "${response.headerMap.head._1}": "${response.headerMap.head._2}"
      }
    }
  }]
}""")

    val suiteName = "my%20suite"

    server.httpPost(
      path = s"/DynamockTest/expectations-suite/store?suite_name=$suiteName",
      postBody = "",
      andExpect = Status.NoContent)

    server.httpDelete(
      path = "/DynamockTest/expectations",
      andExpect = Status.NoContent)

    server.httpGet(
      "/DynamockTest/expectations",
      andExpect = Status.Ok,
      withJsonBody = s"""{ "expectation_responses": [] }""")

    server.httpPost(
      path = s"/DynamockTest/expectations-suite/load?suite_name=$suiteName",
      postBody = "",
      andExpect = Status.Ok,
      withJsonBody =
        s"""{
           |  "suite_load_info": [{
           |    "expectation_id": "$expectationId"
           |  }]
           |}""".stripMargin)

    server.httpGet(
      "/DynamockTest/expectations",
      andExpect = Status.Ok,
      withJsonBody =
        s"""{
  "expectation_responses": [{
    "expectation_id": "$expectationId",
    "expectation": {
      "path": "${expectation.path}",
      "method": "${expectation.method}",
      "content": "${expectation.content.stringValue}",
      "included_header_parameters": {},
      "excluded_header_parameters": {},
      "query_parameters": {}
    },
    "response": {
      "status": ${response.status},
      "content": "${response.content}",
      "header_map": {
        "${response.headerMap.head._1}": "${response.headerMap.head._2}"
      }
    }
  }]
}""")
  }

  test("Hit-count works") {
    val putExpectationsResponse = server.httpPut(
      path = "/DynamockTest/expectations",
      putBody = expectationPutRequestJson(response),
      andExpect = Status.Ok)

    val responseMap = parse(putExpectationsResponse.contentString).filterField(_ => true).toMap
    val expectationInfoJArray = responseMap("expectations_info")
    val expectationInfoSeq = expectationInfoJArray.values.asInstanceOf[::[Map[String, Any]]]
    val expectationInfoMap = expectationInfoSeq.head
    val expectationId = expectationInfoMap("expectation_id").asInstanceOf[String]

    getAndVerifyHitCount(expectationId, 0)

    server.httpPut(
      expectation.path,
      putBody = expectation.content.stringValue,
      andExpect = Status(response.status),
      withBody = response.content)

    getAndVerifyHitCount(expectationId, 1)

    server.httpPut(
      expectation.path,
      putBody = expectation.content.stringValue,
      andExpect = Status(response.status),
      withBody = response.content)

    getAndVerifyHitCount(expectationId, 2)
  }

  test("Hit-count reset works") {
    val putExpectationsResponse = server.httpPut(
      path = "/DynamockTest/expectations",
      putBody = expectationPutRequestJson(response),
      andExpect = Status.Ok)

    val responseMap = parse(putExpectationsResponse.contentString).filterField(_ => true).toMap
    val expectationInfoJArray = responseMap("expectations_info")
    val expectationInfoSeq = expectationInfoJArray.values.asInstanceOf[::[Map[String, Any]]]
    val expectationInfoMap = expectationInfoSeq.head
    val expectationId = expectationInfoMap("expectation_id").asInstanceOf[String]

    getAndVerifyHitCount(expectationId, 0)

    server.httpPut(
      expectation.path,
      putBody = expectation.content.stringValue,
      andExpect = Status(response.status),
      withBody = response.content)

    getAndVerifyHitCount(expectationId, 1)

    server.httpPost(
      path = "/DynamockTest/hit-counts/reset",
      postBody = s"""{ "expectation_ids": ["$expectationId"] }""",
      andExpect = Status.NoContent)

    getAndVerifyHitCount(expectationId, 0)

    server.httpPut(
      expectation.path,
      putBody = expectation.content.stringValue,
      andExpect = Status(response.status),
      withBody = response.content)

    getAndVerifyHitCount(expectationId, 1)
  }

  test("Hit-count does not reset for new response") {
    val putExpectationsResponse = server.httpPut(
      path = "/DynamockTest/expectations",
      putBody = expectationPutRequestJson(response),
      andExpect = Status.Ok)

    val responseMap = parse(putExpectationsResponse.contentString).filterField(_ => true).toMap
    val expectationInfoJArray = responseMap("expectations_info")
    val expectationInfoSeq = expectationInfoJArray.values.asInstanceOf[::[Map[String, Any]]]
    val expectationInfoMap = expectationInfoSeq.head
    val expectationId = expectationInfoMap("expectation_id").asInstanceOf[String]

    getAndVerifyHitCount(expectationId, 0)

    server.httpPut(
      expectation.path,
      putBody = expectation.content.stringValue,
      andExpect = Status(response.status),
      withBody = response.content)

    getAndVerifyHitCount(expectationId, 1)

    server.httpPut(
      path = "/DynamockTest/expectations",
      putBody = expectationPutRequestJson(response2),
      andExpect = Status.Ok)

    getAndVerifyHitCount(expectationId, 1)

    server.httpPut(
      expectation.path,
      putBody = expectation.content.stringValue,
      andExpect = Status(response2.status),
      withBody = response2.content)

    getAndVerifyHitCount(expectationId, 2)
  }

  private def getAndVerifyHitCount(expectationId: String, hitCount: Int): Unit = server.httpPost(
    path = "/DynamockTest/hit-counts/get",
    postBody = s"""{ "expectation_ids": ["$expectationId"] }""",
    andExpect = Status.Ok,
    withJsonBody =
      s"""{
         |  "expectation_id_to_hit_count": {
         |    "$expectationId": $hitCount
         |  }
         |}""".stripMargin
  )
}
