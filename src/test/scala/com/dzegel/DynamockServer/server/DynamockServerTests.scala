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
    args = Seq("-http.port=:1235", "-expectations.path.base=DynamockTest")
  )

  private val expectation = Expectation("PUT", "/some/path", Map.empty, HeaderParameters(Set.empty, Set.empty), Content("someContent"))
  private val response = Response(201, "SomeOtherContent", Map("SomeKey" -> "SomeValue"))
  private val setupName = "setup name"
  private val expectationPutRequestJson =
    s"""
{
  "expectation_responses": [{
    "setup_name": "$setupName",
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
    val setupRespones = server.httpPut(
      path = "/DynamockTest/expectations",
      putBody = expectationPutRequestJson,
      andExpect = Status.Ok)

    val putResponse = server.httpPut(
      expectation.path,
      putBody = expectation.content.stringValue,
      andExpect = Status(response.status),
      withBody = response.content)

    val setupResponseMap = parse(setupRespones.contentString).filterField(x => true).toMap
    val setupInfoJArray = setupResponseMap("setup_info")
    setupInfoJArray shouldBe a[JArray]
    val setupInfoSeq = setupInfoJArray.values.asInstanceOf[::[Map[String, Any]]]
    setupInfoSeq.size shouldBe 1
    val setupInfoMap = setupInfoSeq.head
    setupInfoMap("client_name") shouldBe setupName
    setupInfoMap("did_overwrite_response") shouldBe false
    setupInfoMap("expectation_id") shouldBe a[String]
    setupInfoMap("expectation_id").asInstanceOf[String] should not be empty

    putResponse.headerMap should contain allElementsOf response.headerMap
  }
}
