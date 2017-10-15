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

class MockControllerTests  extends FeatureTest with MockFactory with Matchers {
  private val mockExpectationService = mock[ExpectationService]

  override protected val server: EmbeddedHttpServer = new EmbeddedHttpServer(
    new HttpServer {
      override protected def configureHttp(router: HttpRouter): Unit = {
        router.add(new MockController(mockExpectationService))
      }
    }
  )

  val response = Response(300, "SomeContent", Map("SomeKey" -> "SomeValue"))

  test("GET /somePath should call get response the response") {
    val expectation = Expectation("GET", "/somePath", Content(""))
    setup_ExpectationService_GetResponse(expectation, Success(Some(response)))

    val result = server.httpGet(
      path = "/somePath",
      andExpect = Status(response.status),
      withBody = response.content)

    result.headerMap should contain allElementsOf response.headerMap
  }

  test("POST / should call get response the response") {
    val expectation = Expectation("POST", "/", Content("Some Stuff"))
    setup_ExpectationService_GetResponse(expectation, Success(Some(response)))

    val result = server.httpPost(
      path = "/",
      postBody = expectation.content.stringValue,
      andExpect = Status(response.status),
      withBody = response.content)

    result.headerMap should contain allElementsOf response.headerMap
  }

  test("POST / should return 550 when expectation is not setup") {
    val expectation = Expectation("POST", "/", Content("Some Stuff"))
    setup_ExpectationService_GetResponse(expectation, Success(None))

    server.httpPost(
      path = "/",
      postBody = expectation.content.stringValue,
      andExpect = Status(550),
      withBody = "Dynamock Error: The provided expectation was not setup.")
  }

  test("PUT / should return 5510 when there is an internal error") {
    val expectation = Expectation("PUT", "/", Content("Some Stuff"))
    val errorMessage = "Some Error Message"
    setup_ExpectationService_GetResponse(expectation, Failure(new Exception(errorMessage)))

    server.httpPut(
      path = "/",
      putBody = expectation.content.stringValue,
      andExpect = Status(551),
      withBody = s"Unexpected Dynamock Error: $errorMessage")
  }

  private def setup_ExpectationService_GetResponse(expectation: Expectation, returnValue: Try[Option[Response]]) = {
    (mockExpectationService.getResponse _)
      .expects(expectation)
      .returning(returnValue)
  }
}
