package com.dzegel.DynamockServer.controller

import com.dzegel.DynamockServer.contract.{Expectation, Response, ExpectationSetupPostRequest}
import com.dzegel.DynamockServer.service.ExpectationService
import com.twitter.finagle.http.{Status, Response => TwitterResponse}
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

  private val expectation = Expectation("", "", "")
  private val response = Response()
  private val expectationExpectationPostRequest = ExpectationSetupPostRequest(expectation, response)
  private val expectationSetupPostRequestJson =
    """
{
  "expectation": {
    "path": "",
    "method": "",
    "string_content": ""
  },
  "response": {}
}"""

  test("POST /expectation/setup should call register expectation with ExpectationService and return 204 on success") {
    setup_ExpectationService_RegisterExpectation(expectationExpectationPostRequest, Success(()))

    server.httpPost(
      path = "/expectation/setup",
      postBody = expectationSetupPostRequestJson,
      andExpect = Status.NoContent)
  }

  test("POST /expectation/setup should call register expectation with ExpectationService and return 500 on failure") {
    setup_ExpectationService_RegisterExpectation(expectationExpectationPostRequest, Failure(new Exception))

    server.httpPost(
      path = "/expectation/setup",
      postBody = expectationSetupPostRequestJson,
      andExpect = Status.InternalServerError)
  }

  private def setup_ExpectationService_RegisterExpectation(
    expectationSetupPostRequest: ExpectationSetupPostRequest,
    returnValue: Try[Unit]
  ) = {
    (mockExpectationService.registerExpectation _)
      .expects(expectationSetupPostRequest)
      .returning(returnValue)
  }
}
