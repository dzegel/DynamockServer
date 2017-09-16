package com.dzegel.DynamockServer.server

import com.dzegel.DynamockServer.contract.{Expectation, Response}
import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest

class DynamockServerTests extends FeatureTest {

  override protected val server: EmbeddedHttpServer = new EmbeddedHttpServer(new DynamockServer())

  private val expectation = Expectation("somePath", "get", "someContent")
  private val response = Response()
  private val setupExpectationPostRequestJson =
    s"""
{
  "expectation": {
    "path": "${expectation.path}",
    "method": "${expectation.method}",
    "string_content": "${expectation.stringContent}"
  },
  "response": {}
}"""

  test("POST /setup/expectation should return 204 on success") {
    server.httpPost(
      path = "/setup/expectation",
      postBody = setupExpectationPostRequestJson,
      andExpect = Status.NoContent)
  }
}
