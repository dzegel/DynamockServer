package com.dzegel.DynamockServer.controller

import com.dzegel.DynamockServer.contract.{Expectation, Response, SetupExpectationPostRequest}
import com.dzegel.DynamockServer.service.SetupService
import com.twitter.finagle.http.{Status, Response => TwitterResponse}
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.{EmbeddedHttpServer, HttpServer}
import com.twitter.inject.server.FeatureTest
import org.scalamock.scalatest.MockFactory
import org.scalatest.Matchers

import scala.util.{Failure, Success, Try}

class SetupControllerTests extends FeatureTest with MockFactory with Matchers {

  private val mockSetupService = mock[SetupService]

  override protected val server: EmbeddedHttpServer = new EmbeddedHttpServer(
    new HttpServer {
      override protected def configureHttp(router: HttpRouter): Unit = {
        router.add(new SetupController(mockSetupService))
      }
    }
  )

  private val expectation = Expectation("", "", "")
  private val response = Response()
  private val setupExpectationPostRequest = SetupExpectationPostRequest(expectation, response)
  private val setupExpectationPostRequestJson =
    """
{
  "expectation": {
    "path": "",
    "method": "",
    "string_content": ""
  },
  "response": {}
}"""

  test("POST /setup/expectation should call register expectation with SetupService and return 204 on success") {
    setupSetupServiceRegisterExpectation(setupExpectationPostRequest, Success(()))

    server.httpPost(
      path = "/setup/expectation",
      postBody = setupExpectationPostRequestJson,
      andExpect = Status.NoContent)
  }

  test("POST /setup/expectation should call register expectation with SetupService and return 500 on failure") {
    setupSetupServiceRegisterExpectation(setupExpectationPostRequest, Failure(new Exception))

    server.httpPost(
      path = "/setup/expectation",
      postBody = setupExpectationPostRequestJson,
      andExpect = Status.InternalServerError)
  }

  private def setupSetupServiceRegisterExpectation(
    setupExpectationPostRequest: SetupExpectationPostRequest,
    returnValue: Try[Unit]
  ) = {
    (mockSetupService.registerExpectation _)
      .expects(setupExpectationPostRequest)
      .returning(returnValue)
  }
}
