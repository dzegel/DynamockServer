package com.dzegel.DynamockServer.controller

import com.dzegel.DynamockServer.contract.{Expectation, ExpectationSetupPostRequest}
import com.dzegel.DynamockServer.service.ExpectationService
import com.google.inject.Inject
import com.twitter.finagle.http.{Request, Status}
import com.twitter.finatra.http.Controller

import scala.util.{Failure, Success}

class ExpectationController @Inject()(expectationService: ExpectationService) extends Controller {

  post("/expectation/setup") { request: ExpectationSetupPostRequest =>
    expectationService.registerExpectation(request) match {
      case Success(()) => response.noContent
      case Failure(exception) => response.internalServerError(exception.getMessage)
    }
  }
}
