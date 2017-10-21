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

  private def expectationPutRequestJson(
    expectationPath: String,
    expectationMethod: String,
    expectationContent: Option[String],
    queryParams: Option[Map[String, String]],
    includedHeaderParams: Option[Map[String, String]],
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
    }},
  "response": {
    "status": ${response.status}
  }
}"""

  val response = Response(200, "", Map.empty)

  test("PUT /expectation should call register expectation with ExpectationService and return 204 on success") {
    expectationSetupShouldSucceed("some-path", "POST", Some(Map("query" -> "param")), Some(Map("header" -> "param")), Some("Content"))
    expectationSetupShouldSucceed("some-path", "POST", Some(Map("query" -> "param")), Some(Map("header" -> "param")), None)
    expectationSetupShouldSucceed("some-path", "POST", Some(Map("query" -> "param")), None, Some("Content"))
    expectationSetupShouldSucceed("some-path", "POST", None, Some(Map("header" -> "param")), Some("Content"))
    expectationSetupShouldSucceed("some-path", "POST", None, None, None)
  }

  private def expectationSetupShouldSucceed(
    expectationPath: String,
    expectationMethod: String,
    expectationQueryParams: Option[Map[String, String]],
    expectationIncludedHeaderParams: Option[Map[String, String]],
    expectationContent: Option[String]
  ): Unit = {
    setup_ExpectationService_RegisterExpectation(
      Expectation(
        expectationMethod,
        expectationPath,
        expectationQueryParams.getOrElse(Map.empty),
        expectationIncludedHeaderParams.getOrElse(Map.empty),
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
        response),
      andExpect = Status.NoContent)
  }

  test("PUT /expectation should call register expectation with ExpectationService and return 500 on failure") {
    val expectation = Expectation("POST", "some-path", Map("query" -> "param"), Map("header" -> "param"), Content(""))
    setup_ExpectationService_RegisterExpectation(expectation, response, Failure(new Exception))

    server.httpPut(
      path = "/expectation",
      putBody = expectationPutRequestJson(
        expectation.path,
        expectation.method,
        Some(expectation.content.stringValue),
        Some(expectation.queryParams),
        Some(expectation.includedHeaderParameters),
        response),
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
