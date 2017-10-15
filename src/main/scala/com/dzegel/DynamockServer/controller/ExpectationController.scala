package com.dzegel.DynamockServer.controller

import com.dzegel.DynamockServer.controller.ExpectationController.ExpectationSetupPostRequest
import com.dzegel.DynamockServer.service.ExpectationService
import com.dzegel.DynamockServer.types.{Content, Expectation, Response}
import com.google.inject.Inject
import com.twitter.finatra.http.Controller

import scala.language.implicitConversions
import scala.util.{Failure, Success}

object ExpectationController {

  private case class ExpectationDto(
    method: String,
    path: String,
    stringContent: Option[String],
    jsonContent: Option[String]) {
    //TODO validation method on content fields
  }

  private case class ResponseDto(status: Int, content: Option[String], headerMap: Option[Map[String, String]])

  private case class ExpectationSetupPostRequest(expectation: ExpectationDto, response: ResponseDto)

  private implicit def dtoToExpectation(dto: ExpectationDto): Expectation =
    Expectation(dto.method, dto.path, dto match {
      case ExpectationDto(_, _, None, None) => Content("")
      case ExpectationDto(_, _, Some(stringContent), None) => Content(stringContent)
      case ExpectationDto(_, _, None, Some(jsonContent)) => Content(jsonContent)
      case ExpectationDto(_, _, Some(_), Some(_)) =>
        throw new IllegalArgumentException("At most one content field can be provided.")
    })

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
