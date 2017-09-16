package com.dzegel.DynamockServer.controller

import com.dzegel.DynamockServer.contract.SetupExpectationPostRequest
import com.dzegel.DynamockServer.service.SetupService
import com.google.inject.Inject
import com.twitter.finatra.http.Controller

import scala.util.{Failure, Success}

class SetupController @Inject()(setupService: SetupService) extends Controller {

  post("/setup/expectation"){
    request: SetupExpectationPostRequest =>

      setupService.registerExpectation(request) match {
        case Success(()) => response.noContent
        case Failure(exception) => response.internalServerError(exception)
      }
  }
}
