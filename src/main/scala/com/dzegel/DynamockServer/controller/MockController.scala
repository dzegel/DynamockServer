package com.dzegel.DynamockServer.controller

import com.dzegel.DynamockServer.service.ExpectationService
import com.dzegel.DynamockServer.types.{Content, Request => DynamockRequest}
import com.google.inject.Inject
import com.twitter.finagle.http.{Request => FinagleRequest}
import com.twitter.finatra.http.Controller

import scala.util.{Failure, Success}

class MockController @Inject()(expectationService: ExpectationService) extends Controller {

  any("/:*") { request: FinagleRequest =>
    val expectation = DynamockRequest(
      request.method.name.toUpperCase(),
      request.path,
      request.params.filterKeys(key => key != "*"), // * maps to the request path
      request.headerMap.toSet,
      Content(request.contentString))
    expectationService.getResponse(expectation) match {
      case Success(Some(res)) =>
        response
          .status(res.status)
          .body(res.content)
          .headers(res.headerMap)
      case Success(None) =>
        response.status(551).body("Dynamock Error: The request did not match any registered expectations.")
      case Failure(exception) =>
        response.status(550).body(s"Unexpected Dynamock Server Error: ${exception.getMessage}")
    }
  }
}
