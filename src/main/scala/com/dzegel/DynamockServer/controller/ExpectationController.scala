package com.dzegel.DynamockServer.controller

import com.dzegel.DynamockServer.contract.{Expectation, ExpectationSetupPostRequest}
import com.dzegel.DynamockServer.service.ExpectationService
import com.google.inject.Inject
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller

import scala.util.{Failure, Success}

class ExpectationController @Inject()(expectationService: ExpectationService) extends Controller {

  post("/expectation/setup") { request: ExpectationSetupPostRequest =>
    expectationService.registerExpectation(request) match {
      case Success(()) => response.noContent
      case Failure(exception) => response.internalServerError(exception.getMessage)
    }
  }

  any(":*") { request: Request =>
    val expectation = Expectation(request.method.name, request.path, request.contentString)
    expectationService.getResponse(expectation) match {
      case Success(Some(res)) =>
        response
          .status(res.status)
          .body(res.content)
          .headers(res.headerMap)
      case Success(None) =>
        response.status(550).body("Dynamock Error: The provided expectation was not setup.")
      case Failure(exception) =>
        response.status(551).body(s"Unexpected Dynamock Error: ${exception.getMessage}")
    }
  }
}
