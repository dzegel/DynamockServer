package com.dzegel.DynamockServer.controller

import com.dzegel.DynamockServer.service.ExpectationService
import com.dzegel.DynamockServer.types.{Content, Expectation}
import com.google.inject.Inject
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller

import scala.util.{Failure, Success}

class MockController @Inject()(expectationService: ExpectationService)  extends Controller {

  any(":*") { request: Request =>
    val expectation = Expectation(
      request.method.name,
      request.path,
      request.params.filterKeys(key => key != "*"), // * maps to the request path
      request.headerMap.toMap,
      Content(request.contentString))
    expectationService.getResponse(expectation) match {
      case Success(Some(res)) =>
        response
          .status(res.status)
          .body(res.content)
          .headers(res.headerMap)
      case Success(None) =>
        response.status(551).body("Dynamock Error: The provided expectation was not setup.")
      case Failure(exception) =>
        response.status(550).body(s"Unexpected Dynamock Error: ${exception.getMessage}")
    }
  }
}
