package com.dzegel.DynamockServer.controller

import com.dzegel.DynamockServer.controller.ExpectationsController._
import com.dzegel.DynamockServer.registry.DynamockUrlPathBaseRegistry
import com.dzegel.DynamockServer.service.ExpectationService
import com.dzegel.DynamockServer.service.ExpectationService.RegisterExpectationsInput
import com.dzegel.DynamockServer.types._
import com.google.inject.Inject
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.response.ResponseBuilder
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

  private case class ExpectationsPutRequestItemDto(expectation: ExpectationDto, response: Option[ResponseDto], expectationName: String)

  private case class ExpectationsPutResponseItemDto(expectationId: String, expectationName: String, didOverwriteResponse: Option[Boolean])

  private case class ExpectationsPutRequest(expectationResponses: Set[ExpectationsPutRequestItemDto])

  private case class ExpectationsPutResponse(expectationsInfo: Seq[ExpectationsPutResponseItemDto])

  private case class ExpectationsGetResponseItemDto(expectation: ExpectationDto, response: Option[ResponseDto], expectationId: String)

  private case class ExpectationsGetResponse(expectationResponses: Set[ExpectationsGetResponseItemDto])

  private case class ExpectationsDeleteRequest(expectationIds: Option[Set[String]])

  private case class ExpectationsSuiteStorePostRequest(@QueryParam suiteName: String)

  private case class ExpectationsSuiteLoadPostResponseItemDto(expectationId: String, didOverwriteResponse: Option[Boolean])

  private case class ExpectationsSuiteLoadPostRequest(@QueryParam suiteName: String)

  private case class ExpectationsSuiteLoadPostResponse(suiteLoadInfo: Seq[ExpectationsSuiteLoadPostResponseItemDto])

  private case class HitCountsGetPostRequest(expectationIds: Set[String])

  private case class HitCountsGetPostResponse(expectationIdToHitCount: Map[String, Int])

  private case class HitCountsResetPostRequest(expectationIds: Option[Set[String]])

  private implicit def dtoFromExpectation(expectation: Expectation): ExpectationDto = ExpectationDto(
    expectation.method,
    expectation.path,
    Some(expectation.queryParams),
    Some(expectation.headerParameters.included.toMap),
    Some(expectation.headerParameters.excluded.toMap),
    Some(expectation.content.stringValue)
  )

  private implicit def dtoFromResponse(optionResponse: Option[Response]): Option[ResponseDto] =
    optionResponse.map(response => ResponseDto(response.status, Some(response.content), Some(response.headerMap)))

  private implicit def dtoToExpectation(dto: ExpectationDto): Expectation =
    Expectation(
      dto.method.toUpperCase(),
      dto.path,
      dto.queryParameters.getOrElse(Map.empty),
      HeaderParameters(
        dto.includedHeaderParameters.getOrElse(Map.empty).toSet,
        dto.excludedHeaderParameters.getOrElse(Map.empty).toSet),
      Content(dto.content.getOrElse("")))

  private implicit def dtoToResponse(optionDto: Option[ResponseDto]): Option[Response] =
    optionDto.map(dto => Response(dto.status, dto.content.getOrElse(""), dto.headerMap.getOrElse(Map.empty)))
}

class ExpectationsController @Inject()(
  expectationService: ExpectationService,
  dynamockUrlPathBaseRegistry: DynamockUrlPathBaseRegistry
) extends Controller {
  private val pathBase = dynamockUrlPathBaseRegistry.pathBase
  private val expectationsPathBase = s"$pathBase/expectations"
  private val expectationsSuitePathBase = s"$pathBase/expectations-suite"
  private val hitCountsPathBase = s"$pathBase/hit-counts"

  put(expectationsPathBase) { request: ExpectationsPutRequest =>
    expectationService.registerExpectations(
      request.expectationResponses.map(x => RegisterExpectationsInput(x.expectation, x.response, x.expectationName))
    ).mapToOkResponse(registerExpectationsOutputs =>
      ExpectationsPutResponse(
        registerExpectationsOutputs.map(x => ExpectationsPutResponseItemDto(x.expectationId, x.clientName, x.didOverwriteResponse))
      ))
  }

  delete(expectationsPathBase) { request: ExpectationsDeleteRequest =>
    expectationService.clearExpectations(request.expectationIds).mapToNoContentResponse()
  }

  get(expectationsPathBase) { _: Request =>
    expectationService.getAllExpectations.mapToOkResponse(expectationResponses =>
      ExpectationsGetResponse(expectationResponses.map {
        x => ExpectationsGetResponseItemDto(x.expectation, x.response, x.expectationId)
      }))
  }

  post(s"$expectationsSuitePathBase/store") { request: ExpectationsSuiteStorePostRequest =>
    expectationService.storeExpectations(request.suiteName).mapToNoContentResponse()
  }

  post(s"$expectationsSuitePathBase/load") { request: ExpectationsSuiteLoadPostRequest =>
    expectationService.loadExpectations(request.suiteName).mapToOkResponse(registerExpectationsOutputs =>
      ExpectationsSuiteLoadPostResponse(registerExpectationsOutputs.map { x =>
        ExpectationsSuiteLoadPostResponseItemDto(x.expectationId, x.didOverwriteResponse)
      }))
  }

  post(s"$hitCountsPathBase/get") { request: HitCountsGetPostRequest =>
    expectationService.getHitCounts(request.expectationIds)
      .mapToOkResponse(expectationIdToHitCount => HitCountsGetPostResponse(expectationIdToHitCount))
  }

  post(s"$hitCountsPathBase/reset") { request: HitCountsResetPostRequest =>
    expectationService.resetHitCounts(request.expectationIds).mapToNoContentResponse()
  }

  implicit private class ImplicitEnrichedResponseMapper[T](`try`: Try[T]) {
    def mapToOkResponse[O](responseFunc: T => O): ResponseBuilder#EnrichedResponse = `try` match {
      case Success(in) => response.ok(body = responseFunc(in))
      case Failure(exception) => response.internalServerError(exception.getMessage)
    }

    def mapToNoContentResponse(): ResponseBuilder#EnrichedResponse = `try` match {
      case Success(_) => response.noContent
      case Failure(exception) => response.internalServerError(exception.getMessage)
    }
  }

}
