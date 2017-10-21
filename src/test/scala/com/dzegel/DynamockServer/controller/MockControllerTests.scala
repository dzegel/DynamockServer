package com.dzegel.DynamockServer.controller

import com.dzegel.DynamockServer.service.ExpectationService
import com.dzegel.DynamockServer.types.{Content, Expectation, Response}
import com.twitter.finagle.http.Status
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.{EmbeddedHttpServer, HttpServer}
import com.twitter.inject.server.FeatureTest
import org.scalamock.function.FunctionAdapter1
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

  test("GET /somePath should call getResponse and return the response") {
    val expectation = Expectation("GET", "/somePath", Map(), Map(), Content(""))
    setup_ExpectationService_GetResponse(expectation, Success(Some(response)))

    val result = server.httpGet(
      path = "/somePath",
      andExpect = Status(response.status),
      withBody = response.content)

    result.headerMap should contain allElementsOf response.headerMap
  }

  test("POST / should call getResponse and return the response") {
    val headers = Map("SomeHeader" -> "SomeValue", "SomeOtherHeader" -> "SomeOtherValue")
    val queryParams = Map("QueryParam" -> "Value", "OtherQueryParam" -> "OtherValue")
    val expectation = Expectation("POST", "/", queryParams, headers, Content("Some Stuff"))
    setup_ExpectationService_GetResponse(expectation, Success(Some(response)))

    val result = server.httpPost(
      path = "/?" + queryParams.map(param =>s"""${param._1}=${param._2}""").mkString("&"),
      headers = headers,
      postBody = expectation.content.stringValue,
      andExpect = Status(response.status),
      withBody = response.content)

    result.headerMap should contain allElementsOf response.headerMap
  }

  test("POST / should return 551 when expectation is not setup") {
    val expectation = Expectation("POST", "/", Map.empty, Map.empty, Content("Some Stuff"))
    setup_ExpectationService_GetResponse(expectation, Success(None))

    server.httpPost(
      path = "/",
      postBody = expectation.content.stringValue,
      andExpect = Status(551),
      withBody = "Dynamock Error: The provided expectation was not setup.")
  }

  test("PUT / should return 500 when there is an internal error") {
    val expectation = Expectation("PUT", "/", Map.empty, Map.empty, Content("Some Stuff"))
    val errorMessage = "Some Error Message"
    setup_ExpectationService_GetResponse(expectation, Failure(new Exception(errorMessage)))

    server.httpPut(
      path = "/",
      putBody = expectation.content.stringValue,
      andExpect = Status(550),
      withBody = s"Unexpected Dynamock Error: $errorMessage")
  }

  private def setup_ExpectationService_GetResponse(expectation: Expectation, returnValue: Try[Option[Response]]) = {
    val includedHeaders = expectation.includedHeaderParameters
    (mockExpectationService.getResponse _)
      .expects(new FunctionAdapter1[Expectation, Boolean](exp =>
        includedHeaders.toSet.subsetOf(exp.includedHeaderParameters.toSet) &&
          exp.copy(includedHeaderParameters = includedHeaders) == expectation))
      .returning(returnValue)
  }
}