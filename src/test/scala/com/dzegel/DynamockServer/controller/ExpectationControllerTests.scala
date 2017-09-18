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

  private def expectationSetupPostRequestJson(expectation: Expectation, response: Response) =
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

  {// expectation setup tests
    val expectation = Expectation("", "", "")
    val response = Response(200)
    val expectationExpectationPostRequest = ExpectationSetupPostRequest(expectation, response)

    test("POST /expectation/setup should call register expectation with ExpectationService and return 204 on success") {
      setup_ExpectationService_RegisterExpectation(expectationExpectationPostRequest, Success(()))

      server.httpPost(
        path = "/expectation/setup",
        postBody = expectationSetupPostRequestJson(expectation, response),
        andExpect = Status.NoContent)
    }

    test("POST /expectation/setup should call register expectation with ExpectationService and return 500 on failure") {
      setup_ExpectationService_RegisterExpectation(expectationExpectationPostRequest, Failure(new Exception))

      server.httpPost(
        path = "/expectation/setup",
        postBody = expectationSetupPostRequestJson(expectation, response),
        andExpect = Status.InternalServerError)
    }
  }

  {// mocked expectation tests
    val response = Response(300, "SomeContent", Map("SomeKey" -> "SomeValue"))

    test("GET /somePath should call get response the response") {
      val expectation = Expectation("GET", "/somePath", "")
      setup_ExpectationService_GetResponse(expectation, Success(Some(response)))

      val result = server.httpGet(
        path = "/somePath",
        andExpect = Status(response.status),
        withBody = response.content)

      result.headerMap should contain allElementsOf response.headerMap
    }

    test("POST / should call get response the response") {
      val expectation = Expectation("POST", "/", "Some Stuff")
      setup_ExpectationService_GetResponse(expectation, Success(Some(response)))

      val result = server.httpPost(
        path = "/",
        postBody = expectation.stringContent,
        andExpect = Status(response.status),
        withBody = response.content)

      result.headerMap should contain allElementsOf response.headerMap
    }

    test("POST / should return 550 when expectation is not setup"){
      val expectation = Expectation("POST", "/", "Some Stuff")
      setup_ExpectationService_GetResponse(expectation, Success(None))

      server.httpPost(
        path = "/",
        postBody = expectation.stringContent,
        andExpect = Status(550),
        withBody = "Dynamock Error: The provided expectation was not setup.")
    }

    test("PUT / should return 5510 when there is an internal error"){
      val expectation = Expectation("PUT", "/", "Some Stuff")
      val errorMessage = "Some Error Message"
      setup_ExpectationService_GetResponse(expectation, Failure(new Exception(errorMessage)))

      server.httpPut(
        path = "/",
        putBody = expectation.stringContent,
        andExpect = Status(551),
        withBody = s"Unexpected Dynamock Error: $errorMessage")
    }
  }

  private def setup_ExpectationService_RegisterExpectation(
    expectationSetupPostRequest: ExpectationSetupPostRequest,
    returnValue: Try[Unit]
  ) = {
    (mockExpectationService.registerExpectation _)
      .expects(expectationSetupPostRequest)
      .returning(returnValue)
  }

  private def setup_ExpectationService_GetResponse(expectation: Expectation, returnValue: Try[Option[Response]]) = {
    (mockExpectationService.getResponse _)
      .expects(expectation)
      .returning(returnValue)
  }
}
