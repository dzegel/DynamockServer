package com.dzegel.DynamockServer.controller

import com.dzegel.DynamockServer.controller.ExpectationsController._
import com.dzegel.DynamockServer.registry.ExpectationsUrlPathBaseRegistry
import com.dzegel.DynamockServer.service.ExpectationService
import com.dzegel.DynamockServer.service.ExpectationService.RegisterExpectationsInput
import com.dzegel.DynamockServer.types._
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

  private case class ExpectationResponseDto(expectation: ExpectationDto, response: ResponseDto)

  private case class ExpectationsPutRequestItemDto(expectation: ExpectationDto, response: ResponseDto, setupName: String)

  private case class ExpectationsPutResponseItemDto(expectationId: String, clientName: String, didOverwriteResponse: Boolean)

  private case class ExpectationsPutRequest(expectationResponses: Set[ExpectationsPutRequestItemDto])

  private case class ExpectationsPutResponse(setupInfo: Set[ExpectationsPutResponseItemDto])

  private case class ExpectationsGetResponse(expectationResponses: Set[ExpectationResponseDto])

  private case class ExpectationsSuiteStorePostRequest(@QueryParam suiteName: String)

  private case class ExpectationsSuiteLoadPostRequest(@QueryParam suiteName: String)

  private def expectationResponseToDto(expectationResponse: ExpectationResponse)
  : ExpectationResponseDto = expectationResponse match {
    case (expectation, response) => ExpectationResponseDto(expectation, response)
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
  private val pathBase = expectationsUrlPathBaseRegistry.pathBase

  put(s"$pathBase/expectations") { request: ExpectationsPutRequest =>
    expectationService.registerExpectations(
      request.expectationResponses.map(x => RegisterExpectationsInput(x.expectation: Expectation, x.response: Response, x.setupName))
    ) match {
      case Success(registerExpectationsOutputs) =>
        response.ok(body = ExpectationsPutResponse(
          registerExpectationsOutputs.map(x => ExpectationsPutResponseItemDto(x.expectationId, x.clientName, x.didOverwriteResponse))
        ))
      case Failure(exception) => response.internalServerError(exception.getMessage)
    }
  }

  delete(s"$pathBase/expectations") { _: Request =>
    makeNoContentResponse(expectationService.clearAllExpectations())
  }

  get(s"$pathBase/expectations") { _: Request =>
    expectationService.getAllExpectations match {
      case Success(expectationResponses) =>
        response.ok(body = ExpectationsGetResponse(expectationResponses.map(expectationResponseToDto)))
      case Failure(exception) => response.internalServerError(exception.getMessage)
    }
  }

  post(s"$pathBase/expectations-suite/store") { request: ExpectationsSuiteStorePostRequest =>
    makeNoContentResponse(expectationService.storeExpectations(request.suiteName))
  }

  post(s"$pathBase/expectations-suite/load") { request: ExpectationsSuiteLoadPostRequest =>
    makeNoContentResponse(expectationService.loadExpectations(request.suiteName))
  }

  private def makeNoContentResponse(`try`: Try[Unit]) = `try` match {
    case Success(()) => response.noContent
    case Failure(exception) => response.internalServerError(exception.getMessage)
  }
}
