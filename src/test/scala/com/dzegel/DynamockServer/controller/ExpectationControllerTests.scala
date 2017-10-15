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

  private def expectationSetupPostRequestJson(
    expectationPath: String,
    expectationMethod: String,
    expectationContent: Option[String],
    response: Response) =
    s"""
{
  "expectation": {
    "path": "$expectationPath",
    "method": "$expectationMethod"${
      expectationContent match {
        case Some(content) => s""","content": "$content""""
        case None => ""
      }
    }
  },
  "response": {
    "status": ${response.status}
  }
}"""

  val expectation = Expectation("", "", Content(""))
  val response = Response(200, "", Map.empty)

  test("POST /expectation/setup should call register expectation with ExpectationService and return 204 on success") {
    expectationSetupShouldSucceed("some-path", "POST", Some("Content"))
    expectationSetupShouldSucceed("some-path", "POST", None)
  }

  private def expectationSetupShouldSucceed(
    expectationPath: String,
    expectationMethod: String,
    expectationContent: Option[String]
  ): Unit = {
    setup_ExpectationService_RegisterExpectation(
      Expectation(expectationMethod, expectationPath, Content(expectationContent.getOrElse(""))), response, Success(()))

    server.httpPost(
      path = "/expectation/setup",
      postBody = expectationSetupPostRequestJson(
        expectationPath,
        expectationMethod,
        expectationContent,
        response),
      andExpect = Status.NoContent)
  }

  test("POST /expectation/setup should call register expectation with ExpectationService and return 500 on failure") {
    setup_ExpectationService_RegisterExpectation(expectation, response, Failure(new Exception))

    server.httpPost(
      path = "/expectation/setup",
      postBody = expectationSetupPostRequestJson(
        expectation.path,
        expectation.method,
        Some(expectation.content.stringValue),
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
