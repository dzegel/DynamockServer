package com.dzegel.DynamockServer.controller

import com.dzegel.DynamockServer.registry.{HeaderSet, QueryParams}
import com.dzegel.DynamockServer.service.ExpectationService
import com.dzegel.DynamockServer.types.{Content, Expectation, HeaderParameters, Response}
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

  private def expectationPutRequestJson(
    expectationPath: String,
    expectationMethod: String,
    expectationContent: Option[String],
    queryParams: Option[Map[String, String]],
    includedHeaderParams: Option[Set[(String, String)]],
    excludedHeaderParams: Option[Set[(String, String)]],
    response: Response) =
    s"""
{
  "expectation": {
    "path": "$expectationPath",
    "method": "$expectationMethod"${
      expectationContent match {
        case Some(content) =>
          s""",
    "content": "$content""""
        case None => ""
      }
    }${
      queryParams match {
        case Some(params) =>
          s""",
    "query_parameters":{${params.map(param => s""""${param._1}":"${param._2}"""").mkString(",")}}"""
        case None => ""
      }
    }${
      includedHeaderParams match {
        case Some(params) =>
          s""",
    "included_header_parameters":{${params.map(param => s""""${param._1}":"${param._2}"""").mkString(",")}}"""
        case None => ""
      }
    }${
      excludedHeaderParams match {
        case Some(params) =>
          s""",
    "excluded_header_parameters":{${params.map(param => s""""${param._1}":"${param._2}"""").mkString(",")}}"""
        case None => ""
      }
    }},
  "response": {
    "status": ${response.status}
  }
}"""

  val response = Response(200, "", Map.empty)

  test("PUT /expectation should call register expectation with ExpectationService and return 204 on success") {
    expectationSetupShouldSucceed("some-path", "POST", Some(Map("query" -> "param")), Some(Set("included" -> "includedValue")), Some(Set("excluded" -> "excludedValue")), Some("Content"))
    expectationSetupShouldSucceed("some-path", "POST", Some(Map("query" -> "param")), Some(Set("included" -> "includedValue")), None, Some("Content"))
    expectationSetupShouldSucceed("some-path", "POST", Some(Map("query" -> "param")), Some(Set("included" -> "includedValue")), Some(Set("excluded" -> "excludedValue")), None)
    expectationSetupShouldSucceed("some-path", "POST", Some(Map("query" -> "param")), Some(Set("included" -> "includedValue")), None, None)
    expectationSetupShouldSucceed("some-path", "POST", Some(Map("query" -> "param")), None, Some(Set("excluded" -> "excludedValue")), Some("Content"))
    expectationSetupShouldSucceed("some-path", "POST", Some(Map("query" -> "param")), None, None, Some("Content"))
    expectationSetupShouldSucceed("some-path", "POST", None, Some(Set("included" -> "includedValue")), Some(Set("excluded" -> "excludedValue")), Some("Content"))
    expectationSetupShouldSucceed("some-path", "POST", None, Some(Set("included" -> "includedValue")), None, Some("Content"))
    expectationSetupShouldSucceed("some-path", "POST", None, None, Some(Set("excluded" -> "excludedValue")), None)
    expectationSetupShouldSucceed("some-path", "POST", None, None, None, None)
  }

  private def expectationSetupShouldSucceed(
    expectationPath: String,
    expectationMethod: String,
    expectationQueryParams: Option[QueryParams],
    expectationIncludedHeaderParams: Option[HeaderSet],
    expectationExcludedHeaderParams: Option[HeaderSet],
    expectationContent: Option[String]
  ): Unit = {
    setup_ExpectationService_RegisterExpectation(
      Expectation(
        expectationMethod,
        expectationPath,
        expectationQueryParams.getOrElse(Map.empty),
        HeaderParameters(expectationIncludedHeaderParams.getOrElse(Set.empty), expectationExcludedHeaderParams.getOrElse(Set.empty)),
        Content(expectationContent.getOrElse(""))),
      response,
      Success(()))

    server.httpPut(
      path = "/expectation",
      putBody = expectationPutRequestJson(
        expectationPath,
        expectationMethod,
        expectationContent,
        expectationQueryParams,
        expectationIncludedHeaderParams,
        expectationExcludedHeaderParams,
        response),
      andExpect = Status.NoContent)
  }

  test("PUT /expectation should call register expectation with ExpectationService and return 500 on failure") {
    val expectation = Expectation("POST", "some-path", Map("query" -> "param"), HeaderParameters(Set("included" -> "includedValue"), Set("excluded"-> "excludedValue")), Content(""))
    setup_ExpectationService_RegisterExpectation(expectation, response, Failure(new Exception))

    server.httpPut(
      path = "/expectation",
      putBody = expectationPutRequestJson(
        expectation.path,
        expectation.method,
        Some(expectation.content.stringValue),
        Some(expectation.queryParams),
        Some(expectation.headerParameters.included),
        Some(expectation.headerParameters.excluded),
        response),
      andExpect = Status.InternalServerError)
  }

  test("DELETE /expectations should call clear all expectations with ExpectationService and return 204 on success"){
    setup_ExpectationService_ClearAllExpectations(Success(()))

    server.httpDelete(
      path = "/expectations",
      andExpect = Status.NoContent
    )
  }

  test("DELETE /expectations should call clear all expectations with ExpectationService and return 500 on failure"){
    val errorMessage = "Some Error Message"
    setup_ExpectationService_ClearAllExpectations(Failure(new Exception(errorMessage)))

    server.httpDelete(
      path = "/expectations",
      withBody = errorMessage,
      andExpect = Status.InternalServerError
    )
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

  private def setup_ExpectationService_ClearAllExpectations(returnValue: Try[Unit]) = {
    (mockExpectationService.clearAllExpectations _)
      .expects()
      .returning(returnValue)
  }
}
