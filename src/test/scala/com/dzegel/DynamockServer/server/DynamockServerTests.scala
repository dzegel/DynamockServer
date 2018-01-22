package com.dzegel.DynamockServer.server

import com.dzegel.DynamockServer.types.{Content, Expectation, HeaderParameters, Response}
import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import org.json4s.JsonAST._
import org.json4s.native.JsonParser.parse
import org.scalatest.Matchers

class DynamockServerTests extends FeatureTest with Matchers {

  override protected val server: EmbeddedHttpServer = new EmbeddedHttpServer(
    new DynamockServer {
      override protected lazy val allowUndefinedFlags = true
    }, httpPortFlag = "anything", //setting this to anything other than 'http.port' and setting 'allowUndefinedFlags = true' is a hack that enables the test to pass in the http.port flag defined in args
    args = Seq("-http.port=:1235", "-dynamock.path.base=DynamockTest")
  )

  private val expectation = Expectation("PUT", "/some/path", Map.empty, HeaderParameters(Set.empty, Set.empty), Content("someContent"))
  private val response = Response(201, "SomeOtherContent", Map("SomeKey" -> "SomeValue"))
  private val expectationName = "expectation name"
  private val expectationPutRequestJson =
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

  test("PUT /DynamockTest/expectations returns 204 and the mocked expectation returns the expected response") {
    val putExpectationsResponse = server.httpPut(
      path = "/DynamockTest/expectations",
      putBody = expectationPutRequestJson,
      andExpect = Status.Ok)

    val mockResponse = server.httpPut(
      expectation.path,
      putBody = expectation.content.stringValue,
      andExpect = Status(response.status),
      withBody = response.content)

    val responseMap = parse(putExpectationsResponse.contentString).filterField(x => true).toMap
    val expectationInfoJArray = responseMap("expectations_info")
    expectationInfoJArray shouldBe a[JArray]
    val expectationInfoSeq = expectationInfoJArray.values.asInstanceOf[::[Map[String, Any]]]
    expectationInfoSeq.size shouldBe 1
    val expectationInfoMap = expectationInfoSeq.head
    expectationInfoMap("expectation_name") shouldBe expectationName
    expectationInfoMap("did_overwrite_response") shouldBe false
    expectationInfoMap("expectation_id") shouldBe a[String]
    expectationInfoMap("expectation_id").asInstanceOf[String] should not be empty

    mockResponse.headerMap should contain allElementsOf response.headerMap
  }
}
