package com.dzegel.DynamockServer.server

import com.dzegel.DynamockServer.types._
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

  private val expectation1 = Expectation("PUT", "/some/path/1", Set.empty, HeaderParameters(Set.empty, Set.empty), Content("someContent 1"))
  private val expectation2 = Expectation("POST", "/some/path/2", Set.empty, HeaderParameters(Set.empty, Set.empty), Content("someContent 2"))
  private val response1 = Response(201, "SomeOtherContent", Map("SomeKey" -> "SomeValue"))
  private val response2 = Response(203, "SomeOtherContent2", Map("SomeKey2" -> "SomeValue2"))
  private val expectationName1 = "expectation name 1"
  private val expectationName2 = "expectation name 2"

  private def expectationJson(expectation: Expectation): String =
    s""" "expectation": {
        "path": "${expectation.path}",
        "method": "${expectation.method}",
        "content": "${expectation.content.stringValue}",
        "included_header_parameters": {${paramMapToJsonProps(expectation.headerParameters.included)}},
        "excluded_header_parameters": {${paramMapToJsonProps(expectation.headerParameters.excluded)}},
        "query_parameters": [${expectation.queryParams.map { case (k, v) => s"""{ "key": "$k", "value": "$v" }""" }.mkString(",")}]
      }
    """

  private def responseJson(response: Response): String =
    s""" "response": {
        "status": ${response.status},
        "content": "${response.content}",
        "header_map": {${paramMapToJsonProps(response.headerMap.toSet)}}
      }"""

  private def paramMapToJsonProps(params: Set[(String, String)]): String = params.map { case (k, v) => s""" "$k": "$v" """ }.mkString(",")

  private def expectationRequestItemJson(expectationName: String, expectation: Expectation, response: Option[Response]): String =
    s""" {
      "expectation_name": "$expectationName",
      ${expectationJson(expectation)}
      ${response.map("," + responseJson(_)).getOrElse("")}
}"""

  private def expectationResponseItemJson(expectationId: ExpectationId, expectation: Expectation, response: Option[Response]): String =
    s""" {
      "expectation_id": "$expectationId",
      ${expectationJson(expectation)}
      ${response.map("," + responseJson(_)).getOrElse("")}
}"""

  private def expectationPutRequestJson(expectationResponses: Seq[(String, Expectation, Option[Response])]): String =
    s""" {
  "expectation_responses": [
  ${expectationResponses.map { case (name, exp, res) => expectationRequestItemJson(name, exp, res) }.mkString(",")}
  ]
}"""

  private def expectationPutResponseJson(expectationResponses: Seq[(ExpectationId, Expectation, Option[Response])]): String =
    s""" {
  "expectation_responses": [
  ${expectationResponses.map { case (id, exp, res) => expectationResponseItemJson(id, exp, res) }.mkString(",")}
  ]
}"""

  private def expectationPutRequestJson(response: Option[Response]): String =
    expectationPutRequestJson(Seq((expectationName1, expectation1, response)))

  override protected def beforeEach: Unit = server.httpDelete("/DynamockTest/expectations")

  test("PUT /DynamockTest/expectations - mocked expectation with no response works") {
    server.httpPut(
      path = "/DynamockTest/expectations",
      putBody = expectationPutRequestJson(Seq(
        (expectationName1, expectation1, None),
        (expectationName1 + " constrained", expectation1.copy(headerParameters = HeaderParameters(Set("K" -> "V"), Set.empty)), Some(response1)),
        (expectationName2, expectation2, Some(response2)))),
      andExpect = Status.Ok)

    server.httpPut(
      expectation1.path,
      putBody = expectation1.content.stringValue,
      andExpect = Status(551))

    val result1 = server.httpPut(
      expectation1.path,
      headers = Map("K" -> "V"),
      putBody = expectation1.content.stringValue,
      andExpect = Status(response1.status),
      withBody = response1.content)
    response1.headerMap.foreach { case (k, v) => result1.headerMap(k) shouldBe v }

    val result2 = server.httpPost(
      expectation2.path,
      postBody = expectation2.content.stringValue,
      andExpect = Status(response2.status),
      withBody = response2.content)
    response2.headerMap.foreach { case (k, v) => result2.headerMap(k) shouldBe v }
  }

  test("PUT /DynamockTest/expectations - mocked expectation with duplicated query params works") {
    val expectationA = expectation1.copy(queryParams =  Set("query"->"param","query"->"param2"))
    val expectationB = expectation1.copy(queryParams =  Set("query"->"param","other"->"value" ,"query"->"param2"))

    server.httpPut(
      path = "/DynamockTest/expectations",
      putBody = expectationPutRequestJson(Seq(
        (expectationName1, expectationA, Some(response1)),
        (expectationName2, expectationB, Some(response2)))),
      andExpect = Status.Ok)

    val result1 = server.httpPut(
      s"${expectationA.path}?${expectationA.queryParams.map(x=>s"${x._1}=${x._2}").mkString("&")}",
      putBody = expectationA.content.stringValue,
      andExpect = Status(response1.status),
      withBody = response1.content)
    response1.headerMap.foreach { case (k, v) => result1.headerMap(k) shouldBe v }

    val result2 = server.httpPut(
      s"${expectationB.path}?${expectationB.queryParams.map(x=>s"${x._1}=${x._2}").mkString("&")}",
      putBody = expectationB.content.stringValue,
      andExpect = Status(response2.status),
      withBody = response2.content)
    response2.headerMap.foreach { case (k, v) => result2.headerMap(k) shouldBe v }
  }

  test("PUT /DynamockTest/expectations - mocked expectation - DELETE /DynamockTest/expectations - mocked expectation returns the expected response") {
    val putExpectationsResponse = server.httpPut(
      path = "/DynamockTest/expectations",
      putBody = expectationPutRequestJson(Some(response1)),
      andExpect = Status.Ok)

    val responseMap = parse(putExpectationsResponse.contentString).filterField(_ => true).toMap
    val expectationInfoJArray = responseMap("expectations_info")
    expectationInfoJArray shouldBe a[JArray]
    val expectationInfoSeq = expectationInfoJArray.values.asInstanceOf[::[Map[String, Any]]]
    expectationInfoSeq.size shouldBe 1
    val expectationInfoMap = expectationInfoSeq.head
    expectationInfoMap("expectation_name") shouldBe expectationName1
    expectationInfoMap("did_overwrite_response") shouldBe false
    expectationInfoMap("expectation_id") shouldBe a[String]
    expectationInfoMap("expectation_id").asInstanceOf[String] should not be empty

    val mockResponse = server.httpPut(
      expectation1.path,
      putBody = expectation1.content.stringValue,
      andExpect = Status(response1.status),
      withBody = response1.content)

    mockResponse.headerMap should contain allElementsOf response1.headerMap

    server.httpDelete(
      "/DynamockTest/expectations",
      andExpect = Status.NoContent)

    val mockResponse2 = server.httpPut(
      expectation1.path,
      putBody = expectation1.content.stringValue,
      andExpect = Status(551))

    val mockResponseMap = parse(mockResponse2.contentString).filterField(_ => true).toMap
    val jMessage = mockResponseMap("message")
    jMessage shouldBe JString("Dynamock Error: The request did not match any expectation registered with a response.")
    val mockRequestMap = mockResponseMap("request").values.asInstanceOf[Map[String, Any]]
    mockRequestMap.keySet shouldBe Set("path", "method", "content", "headers", "query_params")
    mockRequestMap("path") shouldBe expectation1.path
    mockRequestMap("method") shouldBe "PUT"
    mockRequestMap("content") shouldBe expectation1.content.stringValue
    mockRequestMap("query_params").asInstanceOf[Seq[Object]] shouldBe empty
  }

  test("PUT /DynamockTest/expectations - mocked expectation - PUT /DynamockTest/expectations with new response - mocked expectation returns the expected response") {
    val putExpectationsResponse = server.httpPut(
      path = "/DynamockTest/expectations",
      putBody = expectationPutRequestJson(Some(response1)),
      andExpect = Status.Ok)

    val responseMap = parse(putExpectationsResponse.contentString).filterField(_ => true).toMap
    val expectationInfoJArray = responseMap("expectations_info")
    expectationInfoJArray shouldBe a[JArray]
    val expectationInfoSeq = expectationInfoJArray.values.asInstanceOf[::[Map[String, Any]]]
    expectationInfoSeq.size shouldBe 1
    val expectationInfoMap = expectationInfoSeq.head
    expectationInfoMap("expectation_name") shouldBe expectationName1
    expectationInfoMap("did_overwrite_response") shouldBe false
    expectationInfoMap("expectation_id") shouldBe a[String]
    val expectationId = expectationInfoMap("expectation_id").asInstanceOf[String]
    expectationId should not be empty

    val mockResponse = server.httpPut(
      expectation1.path,
      putBody = expectation1.content.stringValue,
      andExpect = Status(response1.status),
      withBody = response1.content)

    mockResponse.headerMap should contain allElementsOf response1.headerMap

    server.httpPut(
      path = "/DynamockTest/expectations",
      putBody = expectationPutRequestJson(Some(response2)),
      andExpect = Status.Ok,
      withJsonBody =
        s"""{
           |  "expectations_info": [{
           |    "expectation_name": "$expectationName1",
           |    "expectation_id": "$expectationId",
           |    "did_overwrite_response": true
           |  }]
           |}""".stripMargin)

    val mockResponse2 = server.httpPut(
      expectation1.path,
      putBody = expectation1.content.stringValue,
      andExpect = Status(response2.status),
      withBody = response2.content)

    mockResponse2.headerMap should contain allElementsOf response2.headerMap
  }

  test("PUT - GET - DELETE - GET /DynamockTest/expectations returns the expected output") {
    val putExpectationsResponse = server.httpPut(
      path = "/DynamockTest/expectations",
      putBody = expectationPutRequestJson(Some(response1)),
      andExpect = Status.Ok)

    val responseMap = parse(putExpectationsResponse.contentString).filterField(_ => true).toMap
    val expectationInfoJArray = responseMap("expectations_info")
    expectationInfoJArray shouldBe a[JArray]
    val expectationInfoSeq = expectationInfoJArray.values.asInstanceOf[::[Map[String, Any]]]
    expectationInfoSeq.size shouldBe 1
    val expectationInfoMap = expectationInfoSeq.head
    expectationInfoMap("expectation_name") shouldBe expectationName1
    expectationInfoMap("did_overwrite_response") shouldBe false
    expectationInfoMap("expectation_id") shouldBe a[String]
    val expectationId = expectationInfoMap("expectation_id").asInstanceOf[String]
    expectationId should not be empty

    server.httpGet(
      "/DynamockTest/expectations",
      andExpect = Status.Ok,
      withJsonBody = expectationPutResponseJson(Seq((expectationId, expectation1, Some(response1)))))

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
      putBody = expectationPutRequestJson(Some(response1)),
      andExpect = Status.Ok)

    val responseMap = parse(putExpectationsResponse.contentString).filterField(_ => true).toMap
    val expectationInfoJArray = responseMap("expectations_info")
    val expectationInfoSeq = expectationInfoJArray.values.asInstanceOf[::[Map[String, Any]]]
    val expectationInfoMap = expectationInfoSeq.head
    val expectationId = expectationInfoMap("expectation_id").asInstanceOf[String]

    server.httpGet(
      "/DynamockTest/expectations",
      andExpect = Status.Ok,
      withJsonBody = expectationPutResponseJson(Seq((expectationId, expectation1, Some(response1)))))

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
           |    "expectation_id": "$expectationId",
           |    "did_overwrite_response": false
           |  }]
           |}""".stripMargin)

    server.httpGet(
      "/DynamockTest/expectations",
      andExpect = Status.Ok,
      withJsonBody = expectationPutResponseJson(Seq((expectationId, expectation1, Some(response1)))))
  }

  test("Hit-count works") {
    val putExpectationsResponse = server.httpPut(
      path = "/DynamockTest/expectations",
      putBody = expectationPutRequestJson(Some(response1)),
      andExpect = Status.Ok)

    val responseMap = parse(putExpectationsResponse.contentString).filterField(_ => true).toMap
    val expectationInfoJArray = responseMap("expectations_info")
    val expectationInfoSeq = expectationInfoJArray.values.asInstanceOf[::[Map[String, Any]]]
    val expectationInfoMap = expectationInfoSeq.head
    val expectationId = expectationInfoMap("expectation_id").asInstanceOf[String]

    getAndVerifyHitCount(expectationId, 0)

    server.httpPut(
      expectation1.path,
      putBody = expectation1.content.stringValue,
      andExpect = Status(response1.status),
      withBody = response1.content)

    getAndVerifyHitCount(expectationId, 1)

    server.httpPut(
      expectation1.path,
      putBody = expectation1.content.stringValue,
      andExpect = Status(response1.status),
      withBody = response1.content)

    getAndVerifyHitCount(expectationId, 2)
  }

  test("Hit-count reset works") {
    val putExpectationsResponse = server.httpPut(
      path = "/DynamockTest/expectations",
      putBody = expectationPutRequestJson(Some(response1)),
      andExpect = Status.Ok)

    val responseMap = parse(putExpectationsResponse.contentString).filterField(_ => true).toMap
    val expectationInfoJArray = responseMap("expectations_info")
    val expectationInfoSeq = expectationInfoJArray.values.asInstanceOf[::[Map[String, Any]]]
    val expectationInfoMap = expectationInfoSeq.head
    val expectationId = expectationInfoMap("expectation_id").asInstanceOf[String]

    getAndVerifyHitCount(expectationId, 0)

    server.httpPut(
      expectation1.path,
      putBody = expectation1.content.stringValue,
      andExpect = Status(response1.status),
      withBody = response1.content)

    getAndVerifyHitCount(expectationId, 1)

    server.httpPost(
      path = "/DynamockTest/hit-counts/reset",
      postBody = s"""{ "expectation_ids": ["$expectationId"] }""",
      andExpect = Status.NoContent)

    getAndVerifyHitCount(expectationId, 0)

    server.httpPut(
      expectation1.path,
      putBody = expectation1.content.stringValue,
      andExpect = Status(response1.status),
      withBody = response1.content)

    getAndVerifyHitCount(expectationId, 1)
  }

  test("Hit-count does not reset for new response") {
    val putExpectationsResponse = server.httpPut(
      path = "/DynamockTest/expectations",
      putBody = expectationPutRequestJson(Some(response1)),
      andExpect = Status.Ok)

    val responseMap = parse(putExpectationsResponse.contentString).filterField(_ => true).toMap
    val expectationInfoJArray = responseMap("expectations_info")
    val expectationInfoSeq = expectationInfoJArray.values.asInstanceOf[::[Map[String, Any]]]
    val expectationInfoMap = expectationInfoSeq.head
    val expectationId = expectationInfoMap("expectation_id").asInstanceOf[String]

    getAndVerifyHitCount(expectationId, 0)

    server.httpPut(
      expectation1.path,
      putBody = expectation1.content.stringValue,
      andExpect = Status(response1.status),
      withBody = response1.content)

    getAndVerifyHitCount(expectationId, 1)

    server.httpPut(
      path = "/DynamockTest/expectations",
      putBody = expectationPutRequestJson(Some(response2)),
      andExpect = Status.Ok)

    getAndVerifyHitCount(expectationId, 1)

    server.httpPut(
      expectation1.path,
      putBody = expectation1.content.stringValue,
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
