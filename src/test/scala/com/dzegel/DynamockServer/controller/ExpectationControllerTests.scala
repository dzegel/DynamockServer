package com.dzegel.DynamockServer.controller

import com.dzegel.DynamockServer.service.ExpectationService
import com.dzegel.DynamockServer.types.{Content, Expectation, Response}
import com.twitter.finagle.http.Status
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.{EmbeddedHttpServer, HttpServer}
import com.twitter.inject.server.FeatureTest
import org.scalamock.scalatest.MockFactory
import org.scalatest.Matchers

import scala.util.{Failure, Success, Try}

class ExpectationControllerTests extends FeatureTest with MockFactory with Matchers {

  private val mockExpectationService = mock[ExpectationService]

  override protected val server: EmbeddedHttpServer = new EmbeddedHttpServer(
    new HttpServer {
      override protected def configureHttp(router: HttpRouter): Unit = {
        router.add(new ExpectationController(mockExpectationService))
      }
    }
  )

  private def expectationSetupPostRequestJson(expectation: Expectation, response: Response) =
    s"""
{
  "expectation": {
    "path": "${expectation.path}",
    "method": "${expectation.method}",
    "string_content": "${expectation.content.stringValue}"
  },
  "response": {
    "status": ${response.status}
  }
}"""

  val expectation = Expectation("", "", Content(""))
  val response = Response(200, "", Map.empty)

  test("POST /expectation/setup should call register expectation with ExpectationService and return 204 on success") {
    setup_ExpectationService_RegisterExpectation(expectation, response, Success(()))

    server.httpPost(
      path = "/expectation/setup",
      postBody = expectationSetupPostRequestJson(expectation, response),
      andExpect = Status.NoContent)
  }

  test("POST /expectation/setup should call register expectation with ExpectationService and return 500 on failure") {
    setup_ExpectationService_RegisterExpectation(expectation, response, Failure(new Exception))

    server.httpPost(
      path = "/expectation/setup",
      postBody = expectationSetupPostRequestJson(expectation, response),
      andExpect = Status.InternalServerError)
  }

  private def setup_ExpectationService_RegisterExpectation(
    expectation: Expectation,
    response: Response,
    returnValue: Try[Unit]
  ) = {
    (mockExpectationService.registerExpectation _)
      .expects(expectation, response)
      .returning(returnValue)
  }
}
