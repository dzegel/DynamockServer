package com.dzegel.DynamockServer.controller

import com.dzegel.DynamockServer.controller.ExpectationController.ExpectationSetupPostRequest
import com.dzegel.DynamockServer.service.ExpectationService
import com.dzegel.DynamockServer.types.{Content, Expectation, Response}
import com.google.inject.Inject
import com.twitter.finatra.http.Controller

import scala.language.implicitConversions
import scala.util.{Failure, Success}

object ExpectationController {

  //TODO add 'excludedHeaderParameters'
  private case class ExpectationDto(
    method: String,
    path: String,
    queryParameters: Option[Map[String, String]],
    includedHeaderParameters: Option[Map[String, String]],
    content: Option[String])

  private case class ResponseDto(status: Int, content: Option[String], headerMap: Option[Map[String, String]])

  private case class ExpectationSetupPostRequest(expectation: ExpectationDto, response: ResponseDto)

  private implicit def dtoToExpectation(dto: ExpectationDto): Expectation =
    Expectation(
      dto.method,
      dto.path,
      dto.queryParameters.getOrElse(Map.empty),
      dto.includedHeaderParameters.getOrElse(Map.empty),
      Content(dto.content.getOrElse("")))

  private implicit def dtoToResponse(dto: ResponseDto): Response =
    Response(dto.status, dto.content.getOrElse(""), dto.headerMap.getOrElse(Map.empty))
}

class ExpectationController @Inject()(expectationService: ExpectationService) extends Controller {

  put("/expectation") { request: ExpectationSetupPostRequest =>
    expectationService.registerExpectation(request.expectation, request.response) match {
      case Success(()) => response.noContent
      case Failure(exception) => response.internalServerError(exception.getMessage)
    }
  }
}
