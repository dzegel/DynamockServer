package com.dzegel.DynamockServer.server

import com.dzegel.DynamockServer.contract.{Expectation, Response}
import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest

class DynamockServerTests extends FeatureTest {

  override protected val server: EmbeddedHttpServer = new EmbeddedHttpServer(new DynamockServer())

  private val expectation = Expectation("get", "somePath", "someContent")
  private val response = Response(200)
  private val expectationSetupPostRequestJson =
    s"""
{
  "expectation": {
    "path": "${expectation.path}",
    "method": "${expectation.method}",
    "string_content": "${expectation.stringContent}"
  },
  "response": {
    "status": ${response.status}
  }
}"""

  test("POST /expectation/setup should return 204 on success") {
    server.httpPost(
      path = "/expectation/setup",
      postBody = expectationSetupPostRequestJson,
      andExpect = Status.NoContent)
  }
}
