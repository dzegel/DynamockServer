package com.dzegel.DynamockServer.server

import com.dzegel.DynamockServer.types.{Content, Expectation, HeaderParameters, Response}
import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest

class DynamockServerTests extends FeatureTest {

  override protected val server: EmbeddedHttpServer = new EmbeddedHttpServer(
    new DynamockServer {
      override protected lazy val allowUndefinedFlags = true
    }, httpPortFlag = "anything", //setting this to anything other than 'http.port' and setting 'allowUndefinedFlags = true' is a hack that enables the test to pass in the http.port flag defined in args
    args = Seq("-http.port=:1235", "-expectations.path.base=DynamockTest")
  )

  private val expectation = Expectation("PUT", "/some/path", Map.empty, HeaderParameters(Set.empty, Set.empty), Content("someContent"))
  private val response = Response(201, "SomeOtherContent", Map("SomeKey" -> "SomeValue"))
  private val expectationPutRequestJson =
    s"""
{
  "expectation_response_pairs": [{
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
    server.httpPut(
      path = "/DynamockTest/expectations",
      putBody = expectationPutRequestJson,
      andExpect = Status.NoContent)

    val result = server.httpPut(
      expectation.path,
      putBody = expectation.content.stringValue,
      andExpect = Status(response.status),
      withBody = response.content)

    result.headerMap should contain allElementsOf response.headerMap
  }
}
