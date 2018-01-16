package com.dzegel.DynamockServer.controller

import com.dzegel.DynamockServer.controller.MockController.UnmatchedMockResponse
import com.dzegel.DynamockServer.service.ExpectationService
import com.dzegel.DynamockServer.types.{Content, Request => DynamockRequest}
import com.google.inject.Inject
import com.twitter.finagle.http.{Request => FinagleRequest}
import com.twitter.finatra.http.Controller

import scala.util.{Failure, Success}

object MockController {

  case class UnmatchedMockResponse(message: String, request: DynamockRequest)

}

class MockController @Inject()(expectationService: ExpectationService) extends Controller {

  any("/:*") { finagleRequest: FinagleRequest =>
    val dynamockRequest = DynamockRequest(
      finagleRequest.method.name.toUpperCase(),
      finagleRequest.path,
      finagleRequest.params.filterKeys(key => key != "*"), // * maps to the request path
      finagleRequest.headerMap.toSet,
      Content(finagleRequest.contentString))
    expectationService.getResponse(dynamockRequest) match {
      case Success(Some(res)) =>
        response
          .status(res.status)
          .body(res.content)
          .headers(res.headerMap)
      case Success(None) =>
        response.status(551)
          .body(UnmatchedMockResponse("Dynamock Error: The request did not match any registered expectations.", dynamockRequest))
      case Failure(exception) =>
        response.status(550).body(s"Unexpected Dynamock Server Error: ${exception.getMessage}")
    }
  }
}
