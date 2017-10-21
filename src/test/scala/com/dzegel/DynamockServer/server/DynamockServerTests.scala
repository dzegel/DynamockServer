package com.dzegel.DynamockServer.server

import com.dzegel.DynamockServer.types.{Content, Expectation, Response}
import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest

class DynamockServerTests extends FeatureTest {

  override protected val server: EmbeddedHttpServer = new EmbeddedHttpServer(new DynamockServer())

  private val expectation = Expectation("PUT", "/some/path", Map.empty, Map.empty, Content("someContent"))
  private val response = Response(201, "SomeOtherContent", Map("SomeKey" -> "SomeValue"))
  private val expectationPutRequestJson =
    s"""
{
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
}"""

  test("PUT /expectation returns 204 and the mocked expectation returns the expected response") {
    server.httpPut(
      path = "/expectation",
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
