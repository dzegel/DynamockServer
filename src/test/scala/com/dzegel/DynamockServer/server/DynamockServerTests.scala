package com.dzegel.DynamockServer.server

import com.dzegel.DynamockServer.types.{Content, Expectation, Response}
import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest

class DynamockServerTests extends FeatureTest {

  override protected val server: EmbeddedHttpServer = new EmbeddedHttpServer(new DynamockServer())

  private val expectation = Expectation("PUT", "/some/path", Content("someContent"))
  private val response = Response(201, "SomeOtherContent", Map("SomeKey" -> "SomeValue"))
  private val expectationSetupPostRequestJson =
    s"""
{
  "expectation": {
    "path": "${expectation.path}",
    "method": "${expectation.method}",
    "string_content": "${expectation.content.stringValue}"
  },
  "response": {
    "status": ${response.status},
    "content": "${response.content}",
    "header_map": {
      "${response.headerMap.head._1}": "${response.headerMap.head._2}"
    }
  }
}"""

  test("POST /expectation/setup returns 204 and the mocked expectation returns the expected response") {
    server.httpPost(
      path = "/expectation/setup",
      postBody = expectationSetupPostRequestJson,
      andExpect = Status.NoContent)

    val result = server.httpPut(
      expectation.path,
      putBody = expectation.content.stringValue,
      andExpect = Status(response.status),
      withBody = response.content)

    result.headerMap should contain allElementsOf response.headerMap
  }
}
