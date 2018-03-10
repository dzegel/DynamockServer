package com.dzegel.DynamockServer.service

import com.dzegel.DynamockServer.service.ExpectationService._
import com.dzegel.DynamockServer.service.ExpectationStore._
import com.dzegel.DynamockServer.types._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSuite, Matchers}

import scala.util.{Failure, Success}

class ExpectationServiceTests extends FunSuite with MockFactory with Matchers {

  private val mockExpectationStore = mock[ExpectationStore]
  private val mockExpectationsFileService = mock[ExpectationsFileService]
  private val expectationService = new DefaultExpectationService(mockExpectationStore, mockExpectationsFileService)

  private val expectation = Expectation("POST", "somePath", Map.empty, HeaderParameters(Set.empty, Set.empty), Content(""))
  private val request = Request(expectation.method, expectation.path, expectation.queryParams, expectation.headerParameters.included, expectation.content)
  private val response = Response(200, "", Map.empty)
  private val expectationResponse = (expectation, response)

  private val exception = new Exception("some error message")
  private val expectationSuiteName = "SomeName"

  private val expectationId1 = "id_1"
  private val expectationId2 = "id_2"
  private val expectationIds = Set(expectationId1, expectationId2)

  private val clientName1 = "client name 1"
  private val clientName2 = "client name 2"

  test("registerExpectation returns Success when no Exception is thrown") {
    val expectation2 = expectation.copy(path = "someOtherPath")
    setup_ExpectationStore_RegisterExpectationResponse(
      expectation,
      response,
      Right(RegisterExpectationResponseReturnValue(expectationId1, isResponseUpdated = false))
    )
    setup_ExpectationStore_RegisterExpectationResponse(
      expectation2,
      response,
      Right(RegisterExpectationResponseReturnValue(expectationId2, isResponseUpdated = true))
    )

    expectationService.registerExpectations(Set(
      RegisterExpectationsInput(expectation, response, clientName1),
      RegisterExpectationsInput(expectation2, response, clientName2)
    )) shouldBe Success(Seq(
      RegisterExpectationsOutput(expectationId1, clientName1, didOverwriteResponse = false),
      RegisterExpectationsOutput(expectationId2, clientName2, didOverwriteResponse = true)
    ))
  }

  test("registerExpectation returns Failure on Exception") {
    val exception = new Exception()
    setup_ExpectationStore_RegisterExpectationResponse(expectation, response, Left(exception))

    expectationService.registerExpectations(Set(RegisterExpectationsInput(expectation, response, clientName1))) shouldBe
      Failure(exception)
  }

  test("getResponse returns Success of response") {
    setup_ExpectationStore_GetIdsForMatchingExpectations(request, Right(expectationIds))
    setup_ExpectationStore_GetMostConstrainedExpectationWithId(expectationIds, Right(Some(expectationId1 -> expectationResponse)))

    expectationService.getResponse(request) should equal(Success(Some(response)))
  }

  test("getResponse returns Success of None") {
    setup_ExpectationStore_GetIdsForMatchingExpectations(request, Right(expectationIds))
    setup_ExpectationStore_GetMostConstrainedExpectationWithId(expectationIds, Right(None))

    expectationService.getResponse(request) should equal(Success(None))
  }

  test("getResponse returns Failure when ExpectationStore.getIdsForMatchingExpectations fails") {
    setup_ExpectationStore_GetIdsForMatchingExpectations(request, Left(exception))

    expectationService.getResponse(request) should equal(Failure(exception))
  }

  test("getResponse returns Failure when ExpectationStore.getMostConstrainedExpectationWithId fails") {
    setup_ExpectationStore_GetIdsForMatchingExpectations(request, Right(expectationIds))
    setup_ExpectationStore_GetMostConstrainedExpectationWithId(expectationIds, Left(exception))

    expectationService.getResponse(request) should equal(Failure(exception))
  }

  test("clearExpectations(None) returns Success") {
    setup_ExpectationStore_ClearAllExpectations()

    expectationService.clearExpectations(None) should equal(Success(()))
  }

  test("clearAllExpectations(None) returns Failure") {
    setup_ExpectationStore_ClearAllExpectations(Some(exception))

    expectationService.clearExpectations(None) should equal(Failure(exception))
  }

  test("clearExpectations(Some) returns Success") {
    setup_ExpectationStore_ClearExpectations(expectationIds)

    expectationService.clearExpectations(Some(expectationIds)) should equal(Success(()))
  }

  test("clearAllExpectations(Some) returns Failure") {
    setup_ExpectationStore_ClearExpectations(expectationIds, Some(exception))

    expectationService.clearExpectations(Some(expectationIds)) should equal(Failure(exception))
  }

  test("getAllExpectations returns Success") {
    val returnValue = Set(expectationId1 -> expectationResponse)
    setup_ExpectationStore_GetAllExpectations(Right(returnValue))

    expectationService.getAllExpectations should
      equal(Success(Set(GetExpectationsOutput(expectationId1, expectation, response))))
  }

  test("getAllExpectations returns Failure") {
    setup_ExpectationStore_GetAllExpectations(Left(exception))

    expectationService.getAllExpectations should equal(Failure(exception))
  }

  test("storeExpectations returns Success") {
    val expectationResponses = Set(expectationId1 -> expectationResponse)

    setup_ExpectationStore_GetAllExpectations(Right(expectationResponses))
    setup_ExpectationsFileService_StoreExpectationsAsJson(expectationSuiteName, expectationResponses)

    expectationService.storeExpectations(expectationSuiteName) should equal(Success(()))
  }

  test("storeExpectations returns Failure when get all expectations fails") {
    setup_ExpectationStore_GetAllExpectations(Left(exception))

    expectationService.storeExpectations(expectationSuiteName) should equal(Failure(exception))
  }

  test("storeExpectations returns Failure when store object as json fails") {
    val expectationResponses = Set(expectationId1 -> expectationResponse)

    setup_ExpectationStore_GetAllExpectations(Right(expectationResponses))
    setup_ExpectationsFileService_StoreExpectationsAsJson(expectationSuiteName, expectationResponses, Some(exception))

    expectationService.storeExpectations(expectationSuiteName) should equal(Failure(exception))
  }

  test("loadExpectations returns Success") {
    val oldExpectationId = "some old id"
    val expectationId3 = "id_3"
    val expectation2 = Expectation("2", "2", Map(), null, null)
    val expectation3 = Expectation("3", "3", Map(), null, null)
    val response2 = Response(200, "some content", Map())
    val response3 = Response(300, "some content", Map())
    val expectationResponse2 = expectation2 -> response2
    val expectationResponse3 = expectation3 -> response3
    val expectationResponses = Set(expectationId1 -> expectationResponse, expectationId2 -> expectationResponse2, expectationId3 -> expectationResponse3)
    val storeReturnValue1 = None
    val storeReturnValue2 = Some(RegisterExpectationResponseWithIdReturnValue(oldExpectationId, isResponseUpdated = true))
    val storeReturnValue3 = Some(RegisterExpectationResponseWithIdReturnValue(oldExpectationId, isResponseUpdated = false))
    val serviceReturnValue1 = LoadExpectationsOutput(expectationId1, None)
    val serviceReturnValue2 = LoadExpectationsOutput(expectationId2, Some(LoadExpectationsOverwriteInfo(oldExpectationId, didOverwriteResponse = true)))
    val serviceReturnValue3 = LoadExpectationsOutput(expectationId3, Some(LoadExpectationsOverwriteInfo(oldExpectationId, didOverwriteResponse = false)))

    setup_ExpectationsFileService_LoadExpectationsFromJson(expectationSuiteName, Right(expectationResponses))
    setup_ExpectationStore_RegisterExpectationResponseWithId(expectationId1, expectationResponse, Right(storeReturnValue1))
    setup_ExpectationStore_RegisterExpectationResponseWithId(expectationId2, expectationResponse2, Right(storeReturnValue2))
    setup_ExpectationStore_RegisterExpectationResponseWithId(expectationId3, expectationResponse3, Right(storeReturnValue3))

    expectationService.loadExpectations(expectationSuiteName) shouldBe Success(Seq(serviceReturnValue1, serviceReturnValue2, serviceReturnValue3))
  }

  test("loadExpectations returns Failure when load object from json fails") {
    setup_ExpectationsFileService_LoadExpectationsFromJson(expectationSuiteName, Left(exception))

    expectationService.loadExpectations(expectationSuiteName) should equal(Failure(exception))
  }

  test("loadExpectations returns Failure when register expectation with response fails") {
    val expectation2 = Expectation("2", "2", Map(), null, null)
    val response2 = Response(200, "some content", Map())
    val expectationResponse2 = expectation2 -> response2
    val expectationResponses = Set(expectationId1 -> expectationResponse, expectationId2 -> (expectation2 -> response2))

    setup_ExpectationsFileService_LoadExpectationsFromJson(expectationSuiteName, Right(expectationResponses))
    setup_ExpectationStore_RegisterExpectationResponseWithId(expectationId1, expectationResponse, Right(None))
    setup_ExpectationStore_RegisterExpectationResponseWithId(expectationId2, expectationResponse2, Left(exception))

    expectationService.loadExpectations(expectationSuiteName) should equal(Failure(exception))
  }

  private def setup_ExpectationStore_RegisterExpectationResponse(
    expectation: Expectation,
    response: Response,
    exceptionOrReturnValue: Either[Exception, RegisterExpectationResponseReturnValue]
  ): Unit = {
    val callHandler = (mockExpectationStore.registerExpectationResponse _).expects(expectation -> response)
    exceptionOrReturnValue match {
      case Right(returnValue) => callHandler.returning(returnValue)
      case Left(ex) => callHandler.throwing(ex)
    }
  }

  private def setup_ExpectationStore_RegisterExpectationResponseWithId(
    expectationId: ExpectationId,
    expectationResponse: ExpectationResponse,
    exceptionOrReturnValue: Either[Exception, Option[RegisterExpectationResponseWithIdReturnValue]]
  ): Unit = {
    val callHandler = (mockExpectationStore.registerExpectationResponseWithId _).expects(expectationResponse, expectationId)
    exceptionOrReturnValue match {
      case Right(returnValue) => callHandler.returning(returnValue)
      case Left(ex) => callHandler.throwing(ex)
    }
  }

  private def setup_ExpectationStore_GetIdsForMatchingExpectations(
    request: Request,
    exceptionOrReturnValue: Either[Exception, Set[ExpectationId]]
  ): Unit = {
    val callHandler = (mockExpectationStore.getIdsForMatchingExpectations _).expects(request)
    exceptionOrReturnValue match {
      case Right(returnValue) => callHandler.returning(returnValue)
      case Left(ex) => callHandler.throwing(ex)
    }
  }

  private def setup_ExpectationStore_GetMostConstrainedExpectationWithId(
    expectationIds: Set[ExpectationId],
    exceptionOrReturnValue: Either[Exception, Option[(ExpectationId, ExpectationResponse)]]
  ): Unit = {
    val callHandler = (mockExpectationStore.getMostConstrainedExpectationWithId _).expects(expectationIds)
    exceptionOrReturnValue match {
      case Right(returnValue) => callHandler.returning(returnValue)
      case Left(ex) => callHandler.throwing(ex)
    }
  }

  private def setup_ExpectationStore_GetAllExpectations(
    exceptionOrReturnValue: Either[Exception, Set[(ExpectationId, ExpectationResponse)]]
  ): Unit = {
    val callHandler = (mockExpectationStore.getAllExpectations _).expects()
    exceptionOrReturnValue match {
      case Right(returnValue) => callHandler.returning(returnValue)
      case Left(ex) => callHandler.throwing(ex)
    }
  }

  private def setup_ExpectationStore_ClearAllExpectations(
    exception: Option[Exception] = None
  ): Unit = {
    val callHandler = (mockExpectationStore.clearAllExpectations _).expects()
    exception match {
      case None => callHandler.returning(Unit)
      case Some(ex) => callHandler.throwing(ex)
    }
  }

  private def setup_ExpectationStore_ClearExpectations(
    ids: Set[ExpectationId],
    exception: Option[Exception] = None
  ): Unit = {
    val callHandler = (mockExpectationStore.clearExpectations _).expects(ids)
    exception match {
      case None => callHandler.returning(Unit)
      case Some(ex) => callHandler.throwing(ex)
    }
  }

  private def setup_ExpectationsFileService_StoreExpectationsAsJson(
    fileName: String,
    obj: Set[(ExpectationId, ExpectationResponse)],
    exception: Option[Exception] = None
  ): Unit = {
    val callHandler = (mockExpectationsFileService.storeExpectationsAsJson _).expects(fileName, obj)
    exception match {
      case None => callHandler.returning(Unit)
      case Some(ex) => callHandler.throwing(ex)
    }
  }

  private def setup_ExpectationsFileService_LoadExpectationsFromJson(
    fileName: String,
    exceptionOrReturnValue: Either[Exception, Set[(ExpectationId, ExpectationResponse)]]
  ): Unit = {
    val callHandler = (mockExpectationsFileService.loadExpectationsFromJson _).expects(fileName)
    exceptionOrReturnValue match {
      case Right(returnValue) => callHandler.returning(returnValue)
      case Left(ex) => callHandler.throwing(ex)
    }
  }
}
