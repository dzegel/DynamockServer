package com.dzegel.DynamockServer.controller

import com.dzegel.DynamockServer.registry.DynamockUrlPathBaseRegistry
import com.dzegel.DynamockServer.service.ExpectationService
import com.dzegel.DynamockServer.service.ExpectationService._
import com.dzegel.DynamockServer.types._
import com.twitter.finagle.http.Status
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.{EmbeddedHttpServer, HttpServer}
import com.twitter.inject.server.FeatureTest
import org.scalamock.scalatest.MockFactory
import org.scalatest.Matchers

import scala.util.{Failure, Success, Try}

class ExpectationsControllerTests extends FeatureTest with MockFactory with Matchers {

  private val mockExpectationService = mock[ExpectationService]
  private val dynamockUrlPathBaseRegistry = stub[DynamockUrlPathBaseRegistry]
  (dynamockUrlPathBaseRegistry.pathBase _).when().returns("/test")

  override protected val server: EmbeddedHttpServer = new EmbeddedHttpServer(
    new HttpServer {
      override protected def configureHttp(router: HttpRouter): Unit = {
        router.add(new ExpectationsController(mockExpectationService, dynamockUrlPathBaseRegistry))
      }
    }
  )

  val errorMessage = "Some Error Message"
  val expectationName = "expectation name"
  val expectationId = "expectation id"

  private def expectationPutRequestJson(
    expectationPath: String,
    expectationMethod: String,
    expectationContent: Option[String],
    queryParams: Option[Map[String, String]],
    includedHeaderParams: Option[Set[(String, String)]],
    excludedHeaderParams: Option[Set[(String, String)]],
    response: Option[Response]) =
    s"""
{
  "expectation_responses": [{
    "expectation_name": "$expectationName",
    "expectation": {
      "path": "$expectationPath",
      "method": "$expectationMethod"${
      expectationContent match {
        case Some(content) =>
          s""",
      "content": "$content""""
        case None => ""
      }
    }${
      queryParams match {
        case Some(params) =>
          s""",
      "query_parameters":{${params.map(param => s""""${param._1}":"${param._2}"""").mkString(",")}}"""
        case None => ""
      }
    }${
      includedHeaderParams match {
        case Some(params) =>
          s""",
      "included_header_parameters":{${params.map(param => s""""${param._1}":"${param._2}"""").mkString(",")}}"""
        case None => ""
      }
    }${
      excludedHeaderParams match {
        case Some(params) =>
          s""",
      "excluded_header_parameters":{${params.map(param => s""""${param._1}":"${param._2}"""").mkString(",")}}"""
        case None => ""
      }
    }}${response.map(res => s""", "response": { "status": ${res.status} }""").getOrElse("")}
  }]
}"""

  val response = Response(200, "", Map.empty)

  test("PUT /test/expectations should call register expectations with ExpectationService and return 204 on success") {
    expectationSetupShouldSucceed("some-path", "POST", Some(Map("query" -> "param")), Some(Set("included" -> "includedValue")), Some(Set("excluded" -> "excludedValue")), Some("Content"), Some(response))
    expectationSetupShouldSucceed("some-path", "POST", Some(Map("query" -> "param")), Some(Set("included" -> "includedValue")), None, Some("Content"), Some(response))
    expectationSetupShouldSucceed("some-path", "POST", Some(Map("query" -> "param")), Some(Set("included" -> "includedValue")), Some(Set("excluded" -> "excludedValue")), None, Some(response))
    expectationSetupShouldSucceed("some-path", "POST", Some(Map("query" -> "param")), Some(Set("included" -> "includedValue")), None, None, Some(response))
    expectationSetupShouldSucceed("some-path", "POST", Some(Map("query" -> "param")), None, Some(Set("excluded" -> "excludedValue")), Some("Content"), Some(response))
    expectationSetupShouldSucceed("some-path", "POST", Some(Map("query" -> "param")), None, None, Some("Content"), Some(response))
    expectationSetupShouldSucceed("some-path", "POST", None, Some(Set("included" -> "includedValue")), Some(Set("excluded" -> "excludedValue")), Some("Content"), Some(response))
    expectationSetupShouldSucceed("some-path", "POST", None, Some(Set("included" -> "includedValue")), None, Some("Content"), Some(response))
    expectationSetupShouldSucceed("some-path", "POST", None, None, Some(Set("excluded" -> "excludedValue")), None, Some(response))
    expectationSetupShouldSucceed("some-path", "POST", None, None, None, None, Some(response))
    expectationSetupShouldSucceed("some-path", "POST", Some(Map("query" -> "param")), Some(Set("included" -> "includedValue")), Some(Set("excluded" -> "excludedValue")), Some("Content"), None)
    expectationSetupShouldSucceed("some-path", "POST", Some(Map("query" -> "param")), Some(Set("included" -> "includedValue")), None, Some("Content"), None)
    expectationSetupShouldSucceed("some-path", "POST", Some(Map("query" -> "param")), Some(Set("included" -> "includedValue")), Some(Set("excluded" -> "excludedValue")), None, None)
    expectationSetupShouldSucceed("some-path", "POST", Some(Map("query" -> "param")), Some(Set("included" -> "includedValue")), None, None, None)
    expectationSetupShouldSucceed("some-path", "POST", Some(Map("query" -> "param")), None, Some(Set("excluded" -> "excludedValue")), Some("Content"), None)
    expectationSetupShouldSucceed("some-path", "POST", Some(Map("query" -> "param")), None, None, Some("Content"), None)
    expectationSetupShouldSucceed("some-path", "POST", None, Some(Set("included" -> "includedValue")), Some(Set("excluded" -> "excludedValue")), Some("Content"), None)
    expectationSetupShouldSucceed("some-path", "POST", None, Some(Set("included" -> "includedValue")), None, Some("Content"), None)
    expectationSetupShouldSucceed("some-path", "POST", None, None, Some(Set("excluded" -> "excludedValue")), None, None)
    expectationSetupShouldSucceed("some-path", "POST", None, None, None, None, None)
  }

  private def expectationSetupShouldSucceed(
    expectationPath: String,
    expectationMethod: String,
    expectationQueryParams: Option[QueryParams],
    expectationIncludedHeaderParams: Option[HeaderSet],
    expectationExcludedHeaderParams: Option[HeaderSet],
    expectationContent: Option[String],
    response: Option[Response]
  ): Unit = {
    setup_ExpectationService_RegisterExpectations(
      Expectation(
        expectationMethod,
        expectationPath,
        expectationQueryParams.getOrElse(Map.empty),
        HeaderParameters(expectationIncludedHeaderParams.getOrElse(Set.empty), expectationExcludedHeaderParams.getOrElse(Set.empty)),
        Content(expectationContent.getOrElse(""))),
      response,
      Success(Seq(RegisterExpectationsOutput(expectationId, expectationName, didOverwriteResponse = response.map(_ => false)))))

    server.httpPut(
      path = "/test/expectations",
      putBody = expectationPutRequestJson(
        expectationPath,
        expectationMethod,
        expectationContent,
        expectationQueryParams,
        expectationIncludedHeaderParams,
        expectationExcludedHeaderParams,
        response),
      andExpect = Status.Ok,
      withJsonBody =
        s"""{
           |  "expectations_info" : [
           |    {
           |      "expectation_id" : "$expectationId",
           |      "expectation_name" : "$expectationName" ${response.map(_ => ""","did_overwrite_response" : false""").getOrElse("")}
           |    }
           |  ]
           |}""".stripMargin)
  }

  test("PUT /test/expectations should call register expectation with ExpectationService and return 500 on failure") {
    val expectation = Expectation("POST", "some-path", Map("query" -> "param"), HeaderParameters(Set("included" -> "includedValue"), Set("excluded" -> "excludedValue")), Content(""))
    setup_ExpectationService_RegisterExpectations(expectation, Some(response), Failure(new Exception(errorMessage)))

    server.httpPut(
      path = "/test/expectations",
      putBody = expectationPutRequestJson(
        expectation.path,
        expectation.method,
        Some(expectation.content.stringValue),
        Some(expectation.queryParams),
        Some(expectation.headerParameters.included),
        Some(expectation.headerParameters.excluded),
        Some(response)),
      andExpect = Status.InternalServerError,
      withBody = errorMessage)
  }

  test("DELETE /test/expectations with empty body should clear all expectations with ExpectationService and return 204 on success") {
    setup_ExpectationService_ClearExpectations(None, Success(()))

    server.httpDelete(
      path = "/test/expectations",
      andExpect = Status.NoContent
    )
  }

  test("DELETE /test/expectations with empty object in body should clear all expectations with ExpectationService and return 204 on success") {
    setup_ExpectationService_ClearExpectations(None, Success(()))

    server.httpDelete(
      path = "/test/expectations",
      deleteBody = "{}",
      andExpect = Status.NoContent
    )
  }

  test("DELETE /test/expectations with null expectation ids should clear all expectations with ExpectationService and return 204 on success") {
    setup_ExpectationService_ClearExpectations(None, Success(()))

    server.httpDelete(
      path = "/test/expectations",
      deleteBody = """{"expectation_ids": null}""",
      andExpect = Status.NoContent
    )
  }

  test("DELETE /test/expectations with specified expectation ids should clear specified expectations with ExpectationService and return 204 on success") {
    setup_ExpectationService_ClearExpectations(Some(Set(expectationId)), Success(()))

    server.httpDelete(
      path = "/test/expectations",
      deleteBody = s"""{"expectation_ids": ["$expectationId"]}""",
      andExpect = Status.NoContent
    )
  }

  test("DELETE /test/expectations should clear expectations with ExpectationService and return 500 on failure") {
    setup_ExpectationService_ClearExpectations(Some(Set()), Failure(new Exception(errorMessage)))

    server.httpDelete(
      path = "/test/expectations",
      deleteBody = """{"expectation_ids":[]}""",
      withBody = errorMessage,
      andExpect = Status.InternalServerError
    )
  }

  test("GET /test/expectations should call get all expectations with ExpectationService and return 200 on success") {
    val expectation = Expectation(
      "POST",
      "some-path",
      Map("query" -> "param"),
      HeaderParameters(
        Set("included1" -> "includedValue1", "included2" -> "includedValue2"),
        Set("excluded1" -> "excludedValue1", "excluded2" -> "excludedValue2")),
      Content("Some Expectation Content")
    )

    val response = Response(200, "Some Response Content", Map("responseParam" -> "value"))
    setup_ExpectationService_GetAllExpectations(Success(Set(GetExpectationsOutput(expectationId, expectation, response))))

    val jsonResponse =
      s"""{
         |  "expectation_responses" : [
         |    {
         |      "expectation_id": "$expectationId",
         |      "expectation": {
         |        "method": "${expectation.method}",
         |        "path": "${expectation.path}",
         |        "query_parameters": {
         |          "query": "param"
         |        },
         |        "included_header_parameters": {
         |          "included1": "includedValue1",
         |          "included2": "includedValue2"
         |        },
         |        "excluded_header_parameters": {
         |          "excluded1": "excludedValue1",
         |          "excluded2": "excludedValue2"
         |        },
         |        "content" : "${expectation.content.stringValue}"
         |      },
         |      "response": {
         |        "status": ${response.status},
         |        "content": "${response.content}",
         |        "header_map": {
         |          "responseParam": "value"
         |        }
         |      }
         |    }
         |  ]
         |}""".stripMargin

    server.httpGet(
      path = "/test/expectations",
      withJsonBody = jsonResponse,
      andExpect = Status.Ok)
  }

  test("GET /test/expectations should call get all expectations with ExpectationService and return 500 on failure") {
    setup_ExpectationService_GetAllExpectations(Failure(new Exception(errorMessage)))

    server.httpGet(
      path = "/test/expectations",
      withBody = errorMessage,
      andExpect = Status.InternalServerError
    )
  }

  test("POST /test/expectations-suite/store should call store expectation and return 204 on success") {
    val suiteName = "SomeName"
    setup_ExpectationService_StoreExpectations(suiteName, Success(()))

    server.httpPost(
      path = s"/test/expectations-suite/store?suite_name=$suiteName",
      postBody = "",
      andExpect = Status.NoContent
    )
  }

  test("POST /test/expectations-suite/store should call store expectation and return 500 on failure") {
    val suiteName = "SomeName"
    setup_ExpectationService_StoreExpectations(suiteName, Failure(new Exception(errorMessage)))

    server.httpPost(
      path = s"/test/expectations-suite/store?suite_name=$suiteName",
      postBody = "",
      withBody = errorMessage,
      andExpect = Status.InternalServerError
    )
  }

  test("POST /test/expectations-suite/load should call load expectations and return 200 on success") {
    val suiteName = "SomeName"
    val expectationId1 = "id_1"
    val expectationId2 = "id_2"
    val expectationId3 = "id_3"
    val oldExpectationId1 = "old_id_1"
    val oldExpectationId2 = "old_id_2"
    setup_ExpectationService_LoadExpectations(
      suiteName,
      Success(Seq(
        LoadExpectationsOutput(expectationId1, Some(LoadExpectationsOverwriteInfo(oldExpectationId1, didOverwriteResponse = true))),
        LoadExpectationsOutput(expectationId2, Some(LoadExpectationsOverwriteInfo(oldExpectationId2, didOverwriteResponse = false))),
        LoadExpectationsOutput(expectationId3, None)
      ))
    )

    server.httpPost(
      path = s"/test/expectations-suite/load?suite_name=$suiteName",
      postBody = "",
      andExpect = Status.Ok,
      withJsonBody =
        s"""{
           |"suite_load_info": [{
           |    "expectation_id": "$expectationId1",
           |    "overwrite_info": {
           |      "old_expectation_id": "$oldExpectationId1",
           |      "did_overwrite_response": true
           |    }
           |  },{
           |    "expectation_id": "$expectationId2",
           |    "overwrite_info": {
           |      "old_expectation_id": "$oldExpectationId2",
           |      "did_overwrite_response": false
           |    }
           |  },{
           |    "expectation_id": "$expectationId3"
           |  }]
           |}""".stripMargin
    )
  }

  test("POST /test/expectations-suite/load should call load expectations and return 500 on failure") {
    val suiteName = "SomeName"
    setup_ExpectationService_LoadExpectations(suiteName, Failure(new Exception(errorMessage)))

    server.httpPost(
      path = s"/test/expectations-suite/load?suite_name=$suiteName",
      postBody = "",
      withBody = errorMessage,
      andExpect = Status.InternalServerError
    )
  }

  test("POST /test/hit-counts/get should call getHitCounts and return 200 on success") {
    setup_ExpectationService_GetHitCounts(Set(expectationId), Success(Map(expectationId -> 2)))

    server.httpPost(
      path = "/test/hit-counts/get",
      postBody =
        s"""{
           |  "expectation_ids": ["$expectationId"]
           |}""".stripMargin,
      withJsonBody =
        s"""{
           |  "expectation_id_to_hit_count": {
           |    "$expectationId": 2
           |  }
           |}""".stripMargin,
      andExpect = Status.Ok
    )
  }

  test("POST /test/hit-counts/get should call getHitCounts and return 500 on failure") {
    setup_ExpectationService_GetHitCounts(Set(expectationId), Failure(new Exception(errorMessage)))

    server.httpPost(
      path = "/test/hit-counts/get",
      postBody =
        s"""{
           |  "expectation_ids": ["$expectationId"]
           |}""".stripMargin,
      withBody = errorMessage,
      andExpect = Status.InternalServerError
    )
  }

  test("POST /test/hit-counts/reset when specifying expectation_ids should call resetHitCounts and return 204 on success") {
    setup_ExpectationService_ResetHitCounts(Some(Set(expectationId)), Success(()))

    server.httpPost(
      path = "/test/hit-counts/reset",
      postBody =
        s"""{
           |  "expectation_ids": ["$expectationId"]
           |}""".stripMargin,
      andExpect = Status.NoContent
    )
  }

  test("POST /test/hit-counts/reset when not specifying expectation_ids should call resetHitCounts and return 204 on success") {
    setup_ExpectationService_ResetHitCounts(None, Success(()))

    server.httpPost(
      path = "/test/hit-counts/reset",
      postBody =
        """{
          |  "expectation_ids": null
          |}""".stripMargin,
      andExpect = Status.NoContent
    )
  }

  test("POST /test/hit-counts/reset should call resetHitCounts and return 500 on failure") {
    setup_ExpectationService_ResetHitCounts(Some(Set(expectationId)), Failure(new Exception(errorMessage)))

    server.httpPost(
      path = "/test/hit-counts/reset",
      postBody =
        s"""{
           |  "expectation_ids": ["$expectationId"]
           |}""".stripMargin,
      withBody = errorMessage,
      andExpect = Status.InternalServerError
    )
  }

  private def setup_ExpectationService_RegisterExpectations(
    expectation: Expectation,
    response: Option[Response],
    returnValue: Try[Seq[RegisterExpectationsOutput]]
  ): Unit = {
    (mockExpectationService.registerExpectations _)
      .expects(Set(RegisterExpectationsInput(expectation, response, expectationName)))
      .returning(returnValue)
  }

  private def setup_ExpectationService_GetAllExpectations(returnValue: Try[Set[GetExpectationsOutput]]): Unit = {
    (mockExpectationService.getAllExpectations _)
      .expects()
      .returning(returnValue)
  }

  private def setup_ExpectationService_ClearExpectations(expectationIds: Option[Set[ExpectationId]], returnValue: Try[Unit]): Unit = {
    (mockExpectationService.clearExpectations _)
      .expects(expectationIds)
      .returning(returnValue)
  }

  private def setup_ExpectationService_StoreExpectations(suiteName: String, returnValue: Try[Unit]): Unit = {
    (mockExpectationService.storeExpectations _)
      .expects(suiteName)
      .returning(returnValue)
  }

  private def setup_ExpectationService_LoadExpectations(suiteName: String, returnValue: Try[Seq[LoadExpectationsOutput]]): Unit = {
    (mockExpectationService.loadExpectations _)
      .expects(suiteName)
      .returning(returnValue)
  }

  private def setup_ExpectationService_GetHitCounts(expectationIds: Set[ExpectationId], returnValue: Try[Map[ExpectationId, Int]]): Unit = {
    (mockExpectationService.getHitCounts _)
      .expects(expectationIds)
      .returning(returnValue)
  }

  private def setup_ExpectationService_ResetHitCounts(expectationIds: Option[Set[ExpectationId]], returnValue: Try[Unit]): Unit = {
    (mockExpectationService.resetHitCounts _)
      .expects(expectationIds)
      .returning(returnValue)
  }
}
