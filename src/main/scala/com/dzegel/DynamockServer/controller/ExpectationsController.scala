package com.dzegel.DynamockServer.controller

import com.dzegel.DynamockServer.controller.ExpectationsController._
import com.dzegel.DynamockServer.registry.ExpectationsUrlPathBaseRegistry
import com.dzegel.DynamockServer.service.ExpectationService
import com.dzegel.DynamockServer.types.{Content, Expectation, HeaderParameters, Response}
import com.google.inject.Inject
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.request.QueryParam

import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

object ExpectationsController {

  private case class ExpectationDto(
    method: String,
    path: String,
    queryParameters: Option[Map[String, String]],
    includedHeaderParameters: Option[Map[String, String]],
    excludedHeaderParameters: Option[Map[String, String]],
    content: Option[String])

  private case class ResponseDto(status: Int, content: Option[String], headerMap: Option[Map[String, String]])

  private case class ExpectationsPutRequest(expectationResponsePairs: Set[ExpectationsResponsePairDto])

  private case class ExpectationsResponsePairDto(expectation: ExpectationDto, response: ResponseDto)

  private case class ExpectationsGetResponse(expectationResponsePairs: Set[ExpectationsResponsePairDto])

  private case class ExpectationsStorePostRequest(@QueryParam suiteName: String)

  private case class ExpectationsLoadPostRequest(@QueryParam suiteName: String)

  private def expectationsAndResponsePairToDto(expectationsAndResponsePair: (Expectation, Response))
  : ExpectationsResponsePairDto = expectationsAndResponsePair match {
    case (expectation, response) => ExpectationsResponsePairDto(expectation, response)
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

class ExpectationsController @Inject()(
  expectationService: ExpectationService,
  expectationsUrlPathBaseRegistry: ExpectationsUrlPathBaseRegistry
) extends Controller {
  private val pathBase = s"${expectationsUrlPathBaseRegistry.pathBase}/expectations"

  put(pathBase) { request: ExpectationsPutRequest =>
    makeNoContentResponse(expectationService.registerExpectations(
      request.expectationResponsePairs.map(x => (x.expectation: Expectation, x.response: Response))
    ))
  }

  delete(pathBase) { _: Request =>
    makeNoContentResponse(expectationService.clearAllExpectations())
  }

  get(pathBase) { _: Request =>
    expectationService.getAllExpectations match {
      case Success(expectationResponsePairs) =>
        response.ok(body = ExpectationsGetResponse(expectationResponsePairs.map(expectationsAndResponsePairToDto)))
      case Failure(exception) => response.internalServerError(exception.getMessage)
    }
  }

  post(s"$pathBase/store") { request: ExpectationsStorePostRequest => //TODO is this the correct http method?
    makeNoContentResponse(expectationService.storeExpectations(request.suiteName))
  }

  post(s"$pathBase/load") { request: ExpectationsLoadPostRequest => //TODO is this the correct http method?
    makeNoContentResponse(expectationService.loadExpectations(request.suiteName))
  }

  private def makeNoContentResponse(`try`: Try[Unit]) = `try` match {
    case Success(()) => response.noContent
    case Failure(exception) => response.internalServerError(exception.getMessage)
  }
}
