package com.dzegel.DynamockServer.controller

import com.dzegel.DynamockServer.service.ExpectationService
import com.dzegel.DynamockServer.types._
import com.twitter.finagle.http.Status
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.{EmbeddedHttpServer, HttpServer}
import com.twitter.inject.server.FeatureTest
import org.scalamock.function.FunctionAdapter1
import org.scalamock.scalatest.MockFactory
import org.scalatest.Matchers

import scala.util.{Failure, Success, Try}

class MockControllerTests extends FeatureTest with MockFactory with Matchers {
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
    val expectation = Expectation("GET", "/somePath", Map(), HeaderParameters(Set(), Set()), Content(""))
    setup_ExpectationService_GetResponse(expectation, Success(Some(response)))

    val result = server.httpGet(
      path = "/somePath",
      andExpect = Status(response.status),
      withBody = response.content)

    result.headerMap should contain allElementsOf response.headerMap
  }

  test("POST / should call getResponse and return the response") {
    val includedHeaders = Set("IncludedHeader" -> "IncludedHeader")
    val excludedHeaders = Set("ExcludedHeader" -> "ExcludedValue")
    val headers = includedHeaders.toMap + ("SomeOtherHeader" -> "SomeOtherValue")
    val queryParams = Map("QueryParam" -> "Value", "OtherQueryParam" -> "OtherValue")
    val expectation = Expectation("POST", "/", queryParams, HeaderParameters(includedHeaders, excludedHeaders), Content("Some Stuff"))
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
    val expectation = Expectation("POST", "/", Map.empty, HeaderParameters(Set.empty, Set.empty), Content("Some Stuff"))
    setup_ExpectationService_GetResponse(expectation, Success(None))

    server.httpPost(
      path = "/",
      postBody = expectation.content.stringValue,
      andExpect = Status(551),
      withBody = "Dynamock Error: The request did not match any registered expectations.")
  }

  test("PUT / should return 500 when there is an internal error") {
    val expectation = Expectation("PUT", "/", Map.empty, HeaderParameters(Set.empty, Set.empty), Content("Some Stuff"))
    val errorMessage = "Some Error Message"
    setup_ExpectationService_GetResponse(expectation, Failure(new Exception(errorMessage)))

    server.httpPut(
      path = "/",
      putBody = expectation.content.stringValue,
      andExpect = Status(550),
      withBody = s"Unexpected Dynamock Error: $errorMessage")
  }

  private def setup_ExpectationService_GetResponse(expectation: Expectation, returnValue: Try[Option[Response]]) = {
    val includedHeaders = expectation.headerParameters.included
    val excludedHeaders = expectation.headerParameters.excluded
    (mockExpectationService.getResponse _)
      .expects(new FunctionAdapter1[Request, Boolean](req =>
        compareExpectationStoreParameters(expectation, req) &&
          includedHeaders.subsetOf(req.headers) &&
          excludedHeaders.intersect(req.headers).isEmpty))
      .returning(returnValue)
  }

  private def compareExpectationStoreParameters(left: ExpectationStoreParameters, right: ExpectationStoreParameters) =
    left.path == right.path &&
      left.method == right.method &&
      left.queryParams == right.queryParams &&
      left.content == right.content
}
