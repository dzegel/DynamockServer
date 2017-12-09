package com.dzegel.DynamockServer.controller

import com.dzegel.DynamockServer.controller.ExpectationController._
import com.dzegel.DynamockServer.service.ExpectationService
import com.dzegel.DynamockServer.types.{Content, Expectation, HeaderParameters, Response}
import com.google.inject.Inject
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.request.QueryParam

import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

object ExpectationController {

  private case class ExpectationDto(
    method: String,
    path: String,
    queryParameters: Option[Map[String, String]],
    includedHeaderParameters: Option[Map[String, String]],
    excludedHeaderParameters: Option[Map[String, String]],
    content: Option[String])

  private case class ResponseDto(status: Int, content: Option[String], headerMap: Option[Map[String, String]])

  private case class ExpectationPutRequest(expectation: ExpectationDto, response: ResponseDto)

  private case class ExpectationsAndResponsePairDto(expectation: ExpectationDto, response: ResponseDto)

  private case class ExpectationsGetResponse(expectationAndResponsePairs: Set[ExpectationsAndResponsePairDto])

  private case class ExpectationsStorePostRequest(@QueryParam suiteName: String)

  private case class ExpectationsLoadPostRequest(@QueryParam suiteName: String)

  private def expectationsAndResponsePairToDto(expectationsAndResponsePair: (Expectation, Response))
  : ExpectationsAndResponsePairDto = expectationsAndResponsePair match {
    case (expectation, response) => ExpectationsAndResponsePairDto(expectation, response)
  }

  private implicit def dtoFromExpectation(expectation: Expectation): ExpectationDto = ExpectationDto(
    expectation.method,
    expectation.path,
    Some(expectation.queryParams),
    Some(expectation.headerParameters.included.toMap),
    Some(expectation.headerParameters.excluded.toMap),
    Some(expectation.content.stringValue)
  )

  private implicit def dtoFromResponse(response: Response): ResponseDto = ResponseDto(
    response.status,
    Some(response.content),
    Some(response.headerMap)
  )

  private implicit def dtoToExpectation(dto: ExpectationDto): Expectation =
    Expectation(
      dto.method.toUpperCase(),
      dto.path,
      dto.queryParameters.getOrElse(Map.empty),
      HeaderParameters(
        dto.includedHeaderParameters.getOrElse(Map.empty).toSet,
        dto.excludedHeaderParameters.getOrElse(Map.empty).toSet),
      Content(dto.content.getOrElse("")))

  private implicit def dtoToResponse(dto: ResponseDto): Response =
    Response(dto.status, dto.content.getOrElse(""), dto.headerMap.getOrElse(Map.empty))
}

class ExpectationController @Inject()(expectationService: ExpectationService) extends Controller {

  put("/expectation") { request: ExpectationPutRequest =>
    makeNoContentResponse(expectationService.registerExpectation(request.expectation, request.response))
  }

  delete("/expectations") { _: Request =>
    makeNoContentResponse(expectationService.clearAllExpectations())
  }

  get("/expectations") { _: Request =>
    expectationService.getAllExpectations match {
      case Success(expectationAndResponsePairs) =>
        response.ok(body = ExpectationsGetResponse(expectationAndResponsePairs.map(expectationsAndResponsePairToDto)))
      case Failure(exception) => response.internalServerError(exception.getMessage)
    }
  }

  post("/expectations/store") { request: ExpectationsStorePostRequest => //TODO is this the correct http method?
    makeNoContentResponse(expectationService.storeExpectations(request.suiteName))
  }

  post("/expectations/load") { request: ExpectationsLoadPostRequest => //TODO is this the correct http method?
    makeNoContentResponse(expectationService.loadExpectations(request.suiteName))
  }

  private def makeNoContentResponse(`try`: Try[Unit]) = `try` match {
    case Success(()) => response.noContent
    case Failure(exception) => response.internalServerError(exception.getMessage)
  }
}
