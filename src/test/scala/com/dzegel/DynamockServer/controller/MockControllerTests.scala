package com.dzegel.DynamockServer.controller

import com.dzegel.DynamockServer.service.ExpectationService
import com.dzegel.DynamockServer.types._
import com.twitter.finagle.http.Status
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.{EmbeddedHttpServer, HttpServer}
import com.twitter.inject.server.FeatureTest
import org.json4s.JArray
import org.json4s.JsonAST.JString
import org.json4s.native.JsonParser.parse
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

  private val response = Response(300, "SomeContent", Map("SomeKey" -> "SomeValue"))

  test("GET /somePath should call getResponse and return the response") {
    val expectation = Expectation("GET", "/somePath", Set(), HeaderParameters(Set(), Set()), Content(""))
    setup_ExpectationService_GetResponse(expectation, Success(Some(response)))

    val result = server.httpGet(
      path = "/somePath",
      andExpect = Status(response.status),
      withBody = response.content)

    result.headerMap should contain allElementsOf response.headerMap
  }

  test("GET /somePath should call getResponse with duplicate query params and return the response") {
    val expectation = Expectation("GET", "/somePath", Set("query" -> "param", "query" -> "param2"), HeaderParameters(Set(), Set()), Content(""))
    setup_ExpectationService_GetResponse(expectation, Success(Some(response)))
    setup_ExpectationService_GetResponse(expectation, Success(Some(response)))

    val result1 = server.httpGet(
      path = "/somePath?query=param&query=param2",
      andExpect = Status(response.status),
      withBody = response.content)

    result1.headerMap should contain allElementsOf response.headerMap

    // param order should not matter
    val result2 = server.httpGet(
      path = "/somePath?query=param2&query=param",
      andExpect = Status(response.status),
      withBody = response.content)

    result2.headerMap should contain allElementsOf response.headerMap
  }

  test("POST / should call getResponse and return the response") {
    val includedHeaders = Set("IncludedHeader" -> "IncludedHeader")
    val excludedHeaders = Set("ExcludedHeader" -> "ExcludedValue")
    val headers = includedHeaders.toMap + ("SomeOtherHeader" -> "SomeOtherValue")
    val queryParams = Set("QueryParam" -> "Value", "OtherQueryParam" -> "OtherValue")
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
    val urlResource = "someResource"
    val queryParam = "queryParam"
    val queryValue = "queryValue"
    val headerKey = "headerKey"
    val headerValue = "headerValue"
    val content = "Some Stuff"
    val method = "POST"
    val expectation = Expectation(method, s"/$urlResource", Set(queryParam -> queryValue), HeaderParameters(Set.empty, Set.empty), Content(content))
    setup_ExpectationService_GetResponse(expectation, Success(None))

    val response = server.httpPost(
      path = s"/$urlResource?$queryParam=$queryValue",
      headers = Map(headerKey -> headerValue),
      postBody = expectation.content.stringValue,
      andExpect = Status(551))

    val responseMap = parse(response.contentString).filterField(_ => true).toMap
    responseMap("message") shouldBe JString("Dynamock Error: The request did not match any expectation registered with a response.")
    val requestMap = responseMap("request").filterField(_ => true).toMap
    requestMap("method") shouldBe JString(method)
    requestMap("path") shouldBe JString(s"/$urlResource")
    requestMap("content") shouldBe JString(content)
    val queryParamsArray = requestMap("query_parameters").asInstanceOf[JArray].arr
    queryParamsArray should have size 1
    val queryParamsMap = queryParamsArray.head.filterField(_ => true).toMap
    queryParamsMap("key") shouldBe JString(queryParam)
    queryParamsMap("value") shouldBe JString(queryValue)
    val headerParamsMap = requestMap("header_parameters").filterField(_ => true).toMap
    headerParamsMap(headerKey) shouldBe JString(headerValue)
  }

  test("PUT / should return 500 when there is an internal error") {
    val expectation = Expectation("PUT", "/", Set.empty, HeaderParameters(Set.empty, Set.empty), Content("Some Stuff"))
    val errorMessage = "Some Error Message"
    setup_ExpectationService_GetResponse(expectation, Failure(new Exception(errorMessage)))

    server.httpPut(
      path = "/",
      putBody = expectation.content.stringValue,
      andExpect = Status(550),
      withBody = s"Unexpected Dynamock Server Error: $errorMessage")
  }

  private def setup_ExpectationService_GetResponse(expectation: Expectation, returnValue: Try[Option[Response]]) = {
    val includedHeaders = expectation.headerParameters.included
    val excludedHeaders = expectation.headerParameters.excluded
    (mockExpectationService.getResponse _)
      .expects(new FunctionAdapter1[Request, Boolean](req =>
        compareExpectationToRequest(expectation, req) &&
          includedHeaders.subsetOf(req.headers) &&
          excludedHeaders.intersect(req.headers).isEmpty))
      .returning(returnValue)
  }

  private def compareExpectationToRequest(expectation: Expectation, request: Request) =
    expectation.path == request.path &&
      expectation.method == request.method &&
      expectation.queryParams == request.queryParams &&
      expectation.content == request.content
}
