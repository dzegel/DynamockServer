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
  private val mockHitCountService = mock[HitCountService]
  private val expectationService = new DefaultExpectationService(mockExpectationStore, mockExpectationsFileService, mockHitCountService)

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

    setup_HitCountService_Register(Seq(expectationId1, expectationId2))

    expectationService.registerExpectations(Set(
      RegisterExpectationsInput(expectation, response, clientName1),
      RegisterExpectationsInput(expectation2, response, clientName2)
    )) shouldBe Success(Seq(
      RegisterExpectationsOutput(expectationId1, clientName1, didOverwriteResponse = false),
      RegisterExpectationsOutput(expectationId2, clientName2, didOverwriteResponse = true)
    ))
  }

  test("registerExpectation returns Failure on Exception from ExpectationStore") {
    val exception = new Exception()
    setup_ExpectationStore_RegisterExpectationResponse(expectation, response, Left(exception))

    expectationService.registerExpectations(Set(RegisterExpectationsInput(expectation, response, clientName1))) shouldBe
      Failure(exception)
  }

  test("registerExpectation returns Failure on Exception from HitCountService") {
    val exception = new Exception()
    setup_ExpectationStore_RegisterExpectationResponse(
      expectation,
      response,
      Right(RegisterExpectationResponseReturnValue(expectationId1, isResponseUpdated = false)))

    setup_HitCountService_Register(Seq(expectationId1), Some(exception))

    expectationService.registerExpectations(Set(RegisterExpectationsInput(expectation, response, clientName1))) shouldBe
      Failure(exception)
  }

  test("getResponse returns Success of response") {
    setup_ExpectationStore_GetIdsForMatchingExpectations(request, Right(expectationIds))
    setup_HitCountService_Increment(expectationIds.toSeq)
    setup_ExpectationStore_GetMostConstrainedExpectationWithId(expectationIds, Right(Some(expectationId1 -> expectationResponse)))

    expectationService.getResponse(request) should equal(Success(Some(response)))
  }

  test("getResponse returns Success of None") {
    setup_ExpectationStore_GetIdsForMatchingExpectations(request, Right(expectationIds))
    setup_HitCountService_Increment(expectationIds.toSeq)
    setup_ExpectationStore_GetMostConstrainedExpectationWithId(expectationIds, Right(None))

    expectationService.getResponse(request) should equal(Success(None))
  }

  test("getResponse returns Failure when ExpectationStore.getIdsForMatchingExpectations fails") {
    setup_ExpectationStore_GetIdsForMatchingExpectations(request, Left(exception))

    expectationService.getResponse(request) should equal(Failure(exception))
  }

  test("getResponse returns Failure when HitCountService.Increment fails") {
    setup_ExpectationStore_GetIdsForMatchingExpectations(request, Right(expectationIds))
    setup_HitCountService_Increment(expectationIds.toSeq, Some(exception))

    expectationService.getResponse(request) should equal(Failure(exception))
  }

  test("getResponse returns Failure when ExpectationStore.getMostConstrainedExpectationWithId fails") {
    setup_ExpectationStore_GetIdsForMatchingExpectations(request, Right(expectationIds))
    setup_HitCountService_Increment(expectationIds.toSeq)
    setup_ExpectationStore_GetMostConstrainedExpectationWithId(expectationIds, Left(exception))

    expectationService.getResponse(request) should equal(Failure(exception))
  }

  test("clearAllExpectations(None) returns Success") {
    setup_ExpectationStore_ClearAllExpectations()
    setup_HitCountService_DeleteAll()

    expectationService.clearExpectations(None) should equal(Success(()))
  }

  test("clearAllExpectations(None) returns Failure when ExpectationStore.clearAllExpectations fails") {
    setup_ExpectationStore_ClearAllExpectations(Some(exception))

    expectationService.clearExpectations(None) should equal(Failure(exception))
  }

  test("clearAllExpectations(None) returns Failure when HitCountService.deleteAll fails") {
    setup_ExpectationStore_ClearAllExpectations()
    setup_HitCountService_DeleteAll(Some(exception))

    expectationService.clearExpectations(None) should equal(Failure(exception))
  }

  test("clearExpectations(Some) returns Success") {
    setup_ExpectationStore_ClearExpectations(expectationIds)
    setup_HitCountService_Delete(expectationIds.toSeq)

    expectationService.clearExpectations(Some(expectationIds)) should equal(Success(()))
  }

  test("clearExpectations(Some) returns Failure when ExpectationStore.clearExpectations fails") {
    setup_ExpectationStore_ClearExpectations(expectationIds, Some(exception))

    expectationService.clearExpectations(Some(expectationIds)) should equal(Failure(exception))
  }

  test("clearExpectations(Some) returns Failure when HitCountService.delete fails") {
    setup_ExpectationStore_ClearExpectations(expectationIds)
    setup_HitCountService_Delete(expectationIds.toSeq, Some(exception))

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
    val oldExpectationId2 = "some old id 2"
    val expectationId3 = "id_3"
    val expectation2 = Expectation("2", "2", Map(), null, null)
    val expectation3 = Expectation("3", "3", Map(), null, null)
    val response2 = Response(200, "some content", Map())
    val response3 = Response(300, "some content", Map())
    val expectationResponse2 = expectation2 -> response2
    val expectationResponse3 = expectation3 -> response3
    val expectationResponses = Set(expectationId1 -> expectationResponse, expectationId2 -> expectationResponse2, expectationId3 -> expectationResponse3)
    val storeReturnValue1 = None
    val storeReturnValue2 = Some(RegisterExpectationResponseWithIdReturnValue(oldExpectationId2, isResponseUpdated = true))
    val storeReturnValue3 = Some(RegisterExpectationResponseWithIdReturnValue(expectationId3, isResponseUpdated = false))
    val serviceReturnValue1 = LoadExpectationsOutput(expectationId1, None) //register previously unregistered expectation
    val serviceReturnValue2 = LoadExpectationsOutput(expectationId2, Some(LoadExpectationsOverwriteInfo(oldExpectationId2, didOverwriteResponse = true))) //register previously registered expectation with a new id
    val serviceReturnValue3 = LoadExpectationsOutput(expectationId3, Some(LoadExpectationsOverwriteInfo(expectationId3, didOverwriteResponse = false))) //register previously registered expectation with the old id

    setup_ExpectationsFileService_LoadExpectationsFromJson(expectationSuiteName, Right(expectationResponses))
    setup_ExpectationStore_RegisterExpectationResponseWithId(expectationId1, expectationResponse, Right(storeReturnValue1))
    setup_ExpectationStore_RegisterExpectationResponseWithId(expectationId2, expectationResponse2, Right(storeReturnValue2))
    setup_ExpectationStore_RegisterExpectationResponseWithId(expectationId3, expectationResponse3, Right(storeReturnValue3))
    setup_HitCountService_Delete(Seq(oldExpectationId2))
    setup_HitCountService_Register(Seq(expectationId1, expectationId2, expectationId3))

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

  test("loadExpectations returns Failure when HitCountService.Delete fails") {
    val oldExpectationId2 = "some old id"
    val expectation2 = Expectation("2", "2", Map(), null, null)
    val response2 = Response(200, "some content", Map())
    val expectationResponse2 = expectation2 -> response2
    val expectationResponses = Set(expectationId1 -> expectationResponse, expectationId2 -> (expectation2 -> response2))
    val storeReturnValue2 = Some(RegisterExpectationResponseWithIdReturnValue(oldExpectationId2, isResponseUpdated = true))

    setup_ExpectationsFileService_LoadExpectationsFromJson(expectationSuiteName, Right(expectationResponses))
    setup_ExpectationStore_RegisterExpectationResponseWithId(expectationId1, expectationResponse, Right(None))
    setup_ExpectationStore_RegisterExpectationResponseWithId(expectationId2, expectationResponse2, Right(storeReturnValue2))
    setup_HitCountService_Delete(Seq(oldExpectationId2), Some(exception))

    expectationService.loadExpectations(expectationSuiteName) should equal(Failure(exception))
  }

  test("loadExpectations returns Failure when HitCountService.Register fails") {
    val expectation2 = Expectation("2", "2", Map(), null, null)
    val response2 = Response(200, "some content", Map())
    val expectationResponse2 = expectation2 -> response2
    val expectationResponses = Set(expectationId1 -> expectationResponse, expectationId2 -> (expectation2 -> response2))

    setup_ExpectationsFileService_LoadExpectationsFromJson(expectationSuiteName, Right(expectationResponses))
    setup_ExpectationStore_RegisterExpectationResponseWithId(expectationId1, expectationResponse, Right(None))
    setup_ExpectationStore_RegisterExpectationResponseWithId(expectationId2, expectationResponse2, Right(None))
    setup_HitCountService_Delete(Seq())
    setup_HitCountService_Register(Seq(expectationId1, expectationId2), Some(exception))

    expectationService.loadExpectations(expectationSuiteName) should equal(Failure(exception))
  }

  test("getHitCounts returns Success") {
    setup_HitCountService_Get(expectationIds.toSeq, Right(Map(expectationId1 -> 3)))

    expectationService.getHitCounts(expectationIds) shouldBe Success(Map(expectationId1 -> 3))
  }

  test("getHitCounts returns Failure when HitCountService.get fails") {
    setup_HitCountService_Get(expectationIds.toSeq, Left(exception))

    expectationService.getHitCounts(expectationIds) shouldBe Failure(exception)
  }

  test("resetHitCounts resets for all registered ids and returns Success for None input") {
    setup_ExpectationStore_GetAllExpectations(Right(Set(expectationId1 -> expectationResponse, expectationId2 -> expectationResponse)))
    setup_HitCountService_Delete(expectationIds.toSeq)
    setup_HitCountService_Register(expectationIds.toSeq)

    expectationService.resetHitCounts(None) shouldBe Success(())
  }

  test("resetHitCounts only resets only requested ids and returns Success for Some input") {
    setup_ExpectationStore_GetAllExpectations(Right(Set(expectationId1 -> expectationResponse, expectationId2 -> expectationResponse)))
    setup_HitCountService_Delete(Seq(expectationId1))
    setup_HitCountService_Register(Seq(expectationId1))

    expectationService.resetHitCounts(Some(Set(expectationId1))) shouldBe Success(())
  }

  test("resetHitCounts does not reset unregistered ids and returns Success for Some input") {
    val unregisteredId = "not registered"
    setup_ExpectationStore_GetAllExpectations(Right(Set(expectationId1 -> expectationResponse, expectationId2 -> expectationResponse)))
    setup_HitCountService_Delete(Seq())
    setup_HitCountService_Register(Seq())

    expectationService.resetHitCounts(Some(Set(unregisteredId))) shouldBe Success(())
  }

  test("resetHitCounts resets only registered requested ids and returns Success for Some input") {
    val unregisteredId = "not registered"
    setup_ExpectationStore_GetAllExpectations(Right(Set(expectationId1 -> expectationResponse, expectationId2 -> expectationResponse)))
    setup_HitCountService_Delete(Seq(expectationId1))
    setup_HitCountService_Register(Seq(expectationId1))

    expectationService.resetHitCounts(Some(Set(unregisteredId, expectationId1))) shouldBe Success(())
  }

  test("resetHitCounts returns Failure when ExpectationStore.getAllExpectations fails") {
    setup_ExpectationStore_GetAllExpectations(Left(exception))

    expectationService.resetHitCounts(Some(Set(expectationId1))) shouldBe Failure(exception)
  }

  test("resetHitCounts returns Failure when HitCountService.delete fails") {
    setup_ExpectationStore_GetAllExpectations(Right(Set(expectationId1 -> expectationResponse, expectationId2 -> expectationResponse)))
    setup_HitCountService_Delete(Seq(expectationId1), Some(exception))

    expectationService.resetHitCounts(Some(Set(expectationId1))) shouldBe Failure(exception)
  }

  test("resetHitCounts returns Failure when HitCountService.register fails") {
    setup_ExpectationStore_GetAllExpectations(Right(Set(expectationId1 -> expectationResponse, expectationId2 -> expectationResponse)))
    setup_HitCountService_Delete(Seq(expectationId1))
    setup_HitCountService_Register(Seq(expectationId1), Some(exception))

    expectationService.resetHitCounts(Some(Set(expectationId1))) shouldBe Failure(exception)
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

  private def setup_HitCountService_Register(expectationIds: Seq[ExpectationId], exception: Option[Exception] = None): Unit = {
    val callHandler = (mockHitCountService.register _).expects(expectationIds)
    exception match {
      case None => callHandler.returning(())
      case Some(ex) => callHandler.throwing(ex)
    }
  }

  private def setup_HitCountService_DeleteAll(exception: Option[Exception] = None): Unit = {
    val callHandler = (mockHitCountService.deleteAll _).expects()
    exception match {
      case None => callHandler.returning(())
      case Some(ex) => callHandler.throwing(ex)
    }
  }

  private def setup_HitCountService_Delete(expectationIds: Seq[ExpectationId], exception: Option[Exception] = None): Unit = {
    val callHandler = (mockHitCountService.delete _).expects(expectationIds)
    exception match {
      case None => callHandler.returning(())
      case Some(ex) => callHandler.throwing(ex)
    }
  }

  private def setup_HitCountService_Increment(expectationIds: Seq[ExpectationId], exception: Option[Exception] = None): Unit = {
    val callHandler = (mockHitCountService.increment _).expects(expectationIds)
    exception match {
      case None => callHandler.returning(())
      case Some(ex) => callHandler.throwing(ex)
    }
  }

  def setup_HitCountService_Get(
    expectationIds: Seq[ExpectationId],
    exceptionOrReturnValue: Either[Exception, Map[ExpectationId, Int]]
  ): Unit = {
    val callHandler = (mockHitCountService.get _).expects(expectationIds)
    exceptionOrReturnValue match {
      case Right(returnValue) => callHandler.returning(returnValue)
      case Left(ex) => callHandler.throwing(ex)
    }
  }
}
