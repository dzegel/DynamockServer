package com.dzegel.DynamockServer.controller

import com.dzegel.DynamockServer.controller.ExpectationController.ExpectationSetupPostRequest
import com.dzegel.DynamockServer.service.ExpectationService
import com.dzegel.DynamockServer.types.{Content, Expectation, Response}
import com.google.inject.Inject
import com.twitter.finatra.http.Controller

import scala.language.implicitConversions
import scala.util.{Failure, Success}

object ExpectationController {

  private case class ExpectationDto(method: String, path: String, content: Option[String])

  private case class ResponseDto(status: Int, content: Option[String], headerMap: Option[Map[String, String]])

  private case class ExpectationSetupPostRequest(expectation: ExpectationDto, response: ResponseDto)

  private implicit def dtoToExpectation(dto: ExpectationDto): Expectation =
    Expectation(dto.method, dto.path, Content(dto.content.getOrElse("")))

  private implicit def dtoToResponse(dto: ResponseDto): Response =
    Response(dto.status, dto.content.getOrElse(""), dto.headerMap.getOrElse(Map.empty))
}

class ExpectationController @Inject()(expectationService: ExpectationService) extends Controller {

  post("/expectation/setup") { request: ExpectationSetupPostRequest =>
    expectationService.registerExpectation(request.expectation, request.response) match {
      case Success(()) => response.noContent
      case Failure(exception) => response.internalServerError(exception.getMessage)
    }
  }
}
