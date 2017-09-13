package com.dzegel.DynamockServer.controller

import com.dzegel.DynamockServer.Registry.{Expectation, Response}
import com.dzegel.DynamockServer.contract.SetupExpectationPostRequest
import com.dzegel.DynamockServer.service.SetupService
import com.twitter.finagle.http.{Response => TwitterResponse}
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.{EmbeddedHttpServer, HttpServer}
import com.twitter.inject.server.FeatureTest
import org.scalamock.matchers.Matchers
import org.scalamock.scalatest.MockFactory

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
    "content": ""
  },
  "response": {}
}"""

  test("POST /setup/expectation should call register expectation with SetupService and return 204 on success") {
    setupSetupServiceRegisterExpectation(setupExpectationPostRequest, Success(()))

    server.httpPost(path = "/setup/expectation", postBody = setupExpectationPostRequestJson).statusCode should be(204)
  }

  test("POST /setup/expectation should call register expectation with SetupService and return 400 on failure") {
    setupSetupServiceRegisterExpectation(setupExpectationPostRequest, Failure(new Exception))

    server.httpPost(path = "/setup/expectation", postBody = setupExpectationPostRequestJson).statusCode should be(400)
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
