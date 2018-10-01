package com.dzegel.DynamockServer.service

import com.dzegel.DynamockServer.service.ExpectationService._
import com.dzegel.DynamockServer.testUtil._
import com.dzegel.DynamockServer.types._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSuite, Matchers}

import scala.util.{Failure, Success}

class ExpectationServiceTests extends FunSuite with MockFactory with Matchers {

  private val mockExpectationStore = mock[ExpectationStore]
  private val mockExpectationsFileService = mock[ExpectationsFileService]
  private val mockHitCountService = mock[HitCountService]
  private val mockResponseStore = mock[ResponseStore]
  private val expectationService = new DefaultExpectationService(mockExpectationStore, mockExpectationsFileService, mockHitCountService, mockResponseStore)

  private val expectation = Expectation("POST", "somePath", Map.empty, HeaderParameters(Set.empty, Set.empty), Content(""))
  private val request = Request(expectation.method, expectation.path, expectation.queryParams, expectation.headerParameters.included, expectation.content)
  private val response = Response(200, "", Map.empty)

  private val exception = new Exception("some error message")
  private val expectationSuiteName = "SomeName"

  private val expectationId1 = "id_1"
  private val expectationId2 = "id_2"
  private val expectationId3 = "id_3"
  private val expectationIds = Set(expectationId1, expectationId2)

  private val clientName1 = "client name 1"
  private val clientName2 = "client name 2"
  private val clientName3 = "client name 3"

  test("registerExpectation returns Success when no Exception is thrown") {
    val expectation2 = expectation.copy(path = "someOtherPath 2")
    val expectation3 = expectation.copy(path = "someOtherPath 3")
    setup_ExpectationStore_RegisterExpectation(expectation, Right(expectationId1))
    setup_ExpectationStore_RegisterExpectation(expectation2, Right(expectationId2))
    setup_ExpectationStore_RegisterExpectation(expectation3, Right(expectationId3))
    setup_ResponseStore_RegisterResponse(expectationId1, response, Right(false))
    setup_ResponseStore_RegisterResponse(expectationId2, response, Right(true))

    setup_HitCountService_Register(Seq(expectationId1, expectationId2, expectationId3))

    expectationService.registerExpectations(Set(
      RegisterExpectationsInput(expectation, Some(response), clientName1),
      RegisterExpectationsInput(expectation2, Some(response), clientName2),
      RegisterExpectationsInput(expectation3, None, clientName3)
    )) shouldBe Success(Seq(
      RegisterExpectationsOutput(expectationId1, clientName1, didOverwriteResponse = Some(false)),
      RegisterExpectationsOutput(expectationId2, clientName2, didOverwriteResponse = Some(true)),
      RegisterExpectationsOutput(expectationId3, clientName3, didOverwriteResponse = None)
    ))
  }

  test("registerExpectation returns Failure on Exception from ExpectationStore") {
    setup_ExpectationStore_RegisterExpectation(expectation, Left(exception))

    expectationService.registerExpectations(Set(RegisterExpectationsInput(expectation, Some(response), clientName1))) shouldBe
      Failure(exception)
  }

  test("registerExpectation returns Failure on Exception from ResponseStore") {
    setup_ExpectationStore_RegisterExpectation(expectation, Right(expectationId1))
    setup_ResponseStore_RegisterResponse(expectationId1, response, Left(exception))

    expectationService.registerExpectations(Set(RegisterExpectationsInput(expectation, Some(response), clientName1))) shouldBe
      Failure(exception)
  }

  test("registerExpectation returns Failure on Exception from HitCountService") {
    setup_ExpectationStore_RegisterExpectation(expectation, Right(expectationId1))
    setup_ResponseStore_RegisterResponse(expectationId1, response, Right(false))
    setup_HitCountService_Register(Seq(expectationId1), Some(exception))

    expectationService.registerExpectations(Set(RegisterExpectationsInput(expectation, Some(response), clientName1))) shouldBe
      Failure(exception)
  }

  test("getResponse returns Success of response") {
    setup_ExpectationStore_GetIdsForMatchingExpectations(request, Right(expectationIds))
    setup_HitCountService_Increment(expectationIds.toSeq)
    setup_ExpectationStore_GetMostConstrainedExpectationWithId(expectationIds, Right(Some(expectationId1 -> expectation)))
    setup_ResponseStore_GetResponses(Set(expectationId1), Right(Map(expectationId1 -> response)))

    expectationService.getResponse(request) shouldBe Success(Some(response))
  }

  test("getResponse returns Success of None") {
    setup_ExpectationStore_GetIdsForMatchingExpectations(request, Right(expectationIds))
    setup_HitCountService_Increment(expectationIds.toSeq)
    setup_ExpectationStore_GetMostConstrainedExpectationWithId(expectationIds, Right(None))

    expectationService.getResponse(request) shouldBe Success(None)
  }

  test("getResponse returns Failure when ExpectationStore.getIdsForMatchingExpectations fails") {
    setup_ExpectationStore_GetIdsForMatchingExpectations(request, Left(exception))

    expectationService.getResponse(request) shouldBe Failure(exception)
  }

  test("getResponse returns Failure when HitCountService.Increment fails") {
    setup_ExpectationStore_GetIdsForMatchingExpectations(request, Right(expectationIds))
    setup_HitCountService_Increment(expectationIds.toSeq, Some(exception))

    expectationService.getResponse(request) shouldBe Failure(exception)
  }

  test("getResponse returns Failure when ExpectationStore.getMostConstrainedExpectationWithId fails") {
    setup_ExpectationStore_GetIdsForMatchingExpectations(request, Right(expectationIds))
    setup_HitCountService_Increment(expectationIds.toSeq)
    setup_ExpectationStore_GetMostConstrainedExpectationWithId(expectationIds, Left(exception))

    expectationService.getResponse(request) shouldBe Failure(exception)
  }

  test("getResponse returns Failure when ResponseStore.getResponses fails") {
    setup_ExpectationStore_GetIdsForMatchingExpectations(request, Right(expectationIds))
    setup_HitCountService_Increment(expectationIds.toSeq)
    setup_ExpectationStore_GetMostConstrainedExpectationWithId(expectationIds, Right(Some(expectationId1 -> expectation)))
    setup_ResponseStore_GetResponses(Set(expectationId1), Left(exception))

    expectationService.getResponse(request) shouldBe Failure(exception)
  }

  test("clearAllExpectations(None) returns Success") {
    setup_ExpectationStore_ClearAllExpectations()
    setup_ResponseStore_ClearAllResponses()
    setup_HitCountService_DeleteAll()

    expectationService.clearExpectations(None) shouldBe Success(())
  }

  test("clearAllExpectations(None) returns Failure when ExpectationStore.clearAllExpectations fails") {
    setup_ExpectationStore_ClearAllExpectations(Some(exception))

    expectationService.clearExpectations(None) shouldBe Failure(exception)
  }

  test("clearAllExpectations(None) returns Failure when ResponseStore.clearAllResponses fails") {
    setup_ExpectationStore_ClearAllExpectations()
    setup_ResponseStore_ClearAllResponses(Some(exception))

    expectationService.clearExpectations(None) shouldBe Failure(exception)
  }

  test("clearAllExpectations(None) returns Failure when HitCountService.deleteAll fails") {
    setup_ExpectationStore_ClearAllExpectations()
    setup_ResponseStore_ClearAllResponses()
    setup_HitCountService_DeleteAll(Some(exception))

    expectationService.clearExpectations(None) shouldBe Failure(exception)
  }

  test("clearExpectations(Some) returns Success") {
    setup_ExpectationStore_ClearExpectations(expectationIds)
    setup_ResponseStore_DeleteResponses(expectationIds)
    setup_HitCountService_Delete(expectationIds.toSeq)

    expectationService.clearExpectations(Some(expectationIds)) shouldBe Success(())
  }

  test("clearExpectations(Some) returns Failure when ExpectationStore.clearExpectations fails") {
    setup_ExpectationStore_ClearExpectations(expectationIds, Some(exception))

    expectationService.clearExpectations(Some(expectationIds)) shouldBe Failure(exception)
  }

  test("clearExpectations(Some) returns Failure when HitCountService.delete fails") {
    setup_ExpectationStore_ClearExpectations(expectationIds)
    setup_ResponseStore_DeleteResponses(expectationIds)
    setup_HitCountService_Delete(expectationIds.toSeq, Some(exception))

    expectationService.clearExpectations(Some(expectationIds)) shouldBe Failure(exception)
  }

  test("clearExpectations(Some) returns Failure when ResponseStore.deleteResponses fails") {
    setup_ExpectationStore_ClearExpectations(expectationIds)
    setup_ResponseStore_DeleteResponses(expectationIds, Some(exception))

    expectationService.clearExpectations(Some(expectationIds)) shouldBe Failure(exception)
  }

  test("getAllExpectations returns Success") {
    setup_ExpectationStore_GetAllExpectations(Right(Map(expectationId1 -> expectation, expectationId2 -> expectation)))
    setup_ResponseStore_GetResponses(Set(expectationId1, expectationId2), Right(Map(expectationId1 -> response)))

    expectationService.getAllExpectations shouldBe Success(Set(
      GetExpectationsOutput(expectationId1, expectation, Some(response)),
      GetExpectationsOutput(expectationId2, expectation, None)
    ))
  }

  test("getAllExpectations returns Failure when ExpectationStore.getAllExpectations fails") {
    setup_ExpectationStore_GetAllExpectations(Left(exception))

    expectationService.getAllExpectations shouldBe Failure(exception)
  }

  test("getAllExpectations returns Failure when ResponseStore.getResponses fails") {
    setup_ExpectationStore_GetAllExpectations(Right(Map(expectationId1 -> expectation)))
    setup_ResponseStore_GetResponses(Set(expectationId1), Left(exception))

    expectationService.getAllExpectations shouldBe Failure(exception)
  }

  test("storeExpectations returns Success") {
    setup_ExpectationStore_GetAllExpectations(Right(Map(expectationId1 -> expectation, expectationId2 -> expectation)))
    setup_ResponseStore_GetResponses(Set(expectationId1, expectationId2), Right(Map(expectationId1 -> response)))
    setup_ExpectationsFileService_StoreExpectationsAsJson(expectationSuiteName, Set(expectation -> Some(response), expectation -> None))

    expectationService.storeExpectations(expectationSuiteName) shouldBe Success(())
  }

  test("storeExpectations returns Failure when ResponseStore.getResponses fails") {
    setup_ExpectationStore_GetAllExpectations(Right(Map(expectationId1 -> expectation, expectationId2 -> expectation)))
    setup_ResponseStore_GetResponses(Set(expectationId1, expectationId2), Left(exception))

    expectationService.storeExpectations(expectationSuiteName) shouldBe Failure(exception)
  }

  test("storeExpectations returns Failure when ExpectationStore.getAllExpectations fails") {
    setup_ExpectationStore_GetAllExpectations(Left(exception))

    expectationService.storeExpectations(expectationSuiteName) shouldBe Failure(exception)
  }

  test("storeExpectations returns Failure when store object as json fails") {
    setup_ExpectationStore_GetAllExpectations(Right(Map(expectationId1 -> expectation, expectationId2 -> expectation)))
    setup_ResponseStore_GetResponses(Set(expectationId1, expectationId2), Right(Map(expectationId1 -> response)))
    setup_ExpectationsFileService_StoreExpectationsAsJson(expectationSuiteName, Set(expectation -> Some(response), expectation -> None), Some(exception))

    expectationService.storeExpectations(expectationSuiteName) shouldBe Failure(exception)
  }

  test("loadExpectations returns Success") {
    val expectationId4 = "id_4"
    val expectation2 = Expectation("2", "2", Map(), null, null)
    val expectation3 = Expectation("3", "3", Map(), null, null)
    val expectation4 = Expectation("4", "4", Map(), null, null)
    val response2 = Response(200, "some content", Map())
    val response3 = Response(300, "some content", Map())
    val expectationResponses = Set(
      expectation -> Some(response),
      expectation2 -> Some(response2),
      expectation3 -> Some(response3),
      expectation4 -> None)
    val serviceReturnValue1 = LoadExpectationsOutput(expectationId1, Some(false)) //register previously unregistered expectation
    val serviceReturnValue2 = LoadExpectationsOutput(expectationId2, Some(true))
    val serviceReturnValue3 = LoadExpectationsOutput(expectationId3, Some(false))
    val serviceReturnValue4 = LoadExpectationsOutput(expectationId4, None)

    setup_ExpectationsFileService_LoadExpectationsFromJson(expectationSuiteName, Right(expectationResponses))
    setup_ExpectationStore_RegisterExpectation(expectation, Right(expectationId1))
    setup_ExpectationStore_RegisterExpectation(expectation2, Right(expectationId2))
    setup_ExpectationStore_RegisterExpectation(expectation3, Right(expectationId3))
    setup_ExpectationStore_RegisterExpectation(expectation4, Right(expectationId4))
    setup_ResponseStore_RegisterResponse(expectationId1, response, Right(false))
    setup_ResponseStore_RegisterResponse(expectationId2, response2, Right(true))
    setup_ResponseStore_RegisterResponse(expectationId3, response3, Right(false))
    setup_HitCountService_Register(Seq(expectationId1, expectationId2, expectationId3, expectationId4))

    expectationService.loadExpectations(expectationSuiteName) shouldBe
      Success(Seq(serviceReturnValue1, serviceReturnValue2, serviceReturnValue3, serviceReturnValue4))
  }

  test("loadExpectations returns Failure when ExpectationsFileService.loadExpectationsFromJson fails") {
    setup_ExpectationsFileService_LoadExpectationsFromJson(expectationSuiteName, Left(exception))

    expectationService.loadExpectations(expectationSuiteName) shouldBe Failure(exception)
  }

  test("loadExpectations returns Failure when ExpectationStore.registerExpectation fails") {
    val expectation2 = Expectation("2", "2", Map(), null, null)
    val response2 = Response(200, "some content", Map())
    val expectationResponses = Set(expectation -> Option(response), expectation2 -> Option(response2))

    setup_ExpectationsFileService_LoadExpectationsFromJson(expectationSuiteName, Right(expectationResponses))
    setup_ExpectationStore_RegisterExpectation(expectation, Left(exception))

    expectationService.loadExpectations(expectationSuiteName) shouldBe Failure(exception)
  }

  test("loadExpectations returns Failure when ResponseStore.registerResponse fails") {
    val expectationResponses = Set(expectation -> Option(response))

    setup_ExpectationsFileService_LoadExpectationsFromJson(expectationSuiteName, Right(expectationResponses))
    setup_ExpectationStore_RegisterExpectation(expectation, Right(expectationId1))
    setup_ResponseStore_RegisterResponse(expectationId1, response, Left(exception))

    expectationService.loadExpectations(expectationSuiteName) shouldBe Failure(exception)
  }

  test("loadExpectations returns Failure when HitCountService.Register fails") {
    val expectationResponses = Set(expectation -> Option(response))

    setup_ExpectationsFileService_LoadExpectationsFromJson(expectationSuiteName, Right(expectationResponses))
    setup_ExpectationStore_RegisterExpectation(expectation, Right(expectationId1))
    setup_ResponseStore_RegisterResponse(expectationId1, response, Right(false))
    setup_HitCountService_Register(Seq(expectationId1), Some(exception))

    expectationService.loadExpectations(expectationSuiteName) shouldBe Failure(exception)
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
    setup_ExpectationStore_GetAllExpectations(Right(Map(expectationId1 -> expectation, expectationId2 -> expectation)))
    setup_HitCountService_Delete(expectationIds.toSeq)
    setup_HitCountService_Register(expectationIds.toSeq)

    expectationService.resetHitCounts(None) shouldBe Success(())
  }

  test("resetHitCounts only resets only requested ids and returns Success for Some input") {
    setup_ExpectationStore_GetAllExpectations(Right(Map(expectationId1 -> expectation, expectationId2 -> expectation)))
    setup_HitCountService_Delete(Seq(expectationId1))
    setup_HitCountService_Register(Seq(expectationId1))

    expectationService.resetHitCounts(Some(Set(expectationId1))) shouldBe Success(())
  }

  test("resetHitCounts does not reset unregistered ids and returns Success for Some input") {
    val unregisteredId = "not registered"
    setup_ExpectationStore_GetAllExpectations(Right(Map(expectationId1 -> expectation, expectationId2 -> expectation)))
    setup_HitCountService_Delete(Seq())
    setup_HitCountService_Register(Seq())

    expectationService.resetHitCounts(Some(Set(unregisteredId))) shouldBe Success(())
  }

  test("resetHitCounts resets only registered requested ids and returns Success for Some input") {
    val unregisteredId = "not registered"
    setup_ExpectationStore_GetAllExpectations(Right(Map(expectationId1 -> expectation, expectationId2 -> expectation)))
    setup_HitCountService_Delete(Seq(expectationId1))
    setup_HitCountService_Register(Seq(expectationId1))

    expectationService.resetHitCounts(Some(Set(unregisteredId, expectationId1))) shouldBe Success(())
  }

  test("resetHitCounts returns Failure when ExpectationStore.getAllExpectations fails") {
    setup_ExpectationStore_GetAllExpectations(Left(exception))

    expectationService.resetHitCounts(Some(Set(expectationId1))) shouldBe Failure(exception)
  }

  test("resetHitCounts returns Failure when HitCountService.delete fails") {
    setup_ExpectationStore_GetAllExpectations(Right(Map(expectationId1 -> expectation, expectationId2 -> expectation)))
    setup_HitCountService_Delete(Seq(expectationId1), Some(exception))

    expectationService.resetHitCounts(Some(Set(expectationId1))) shouldBe Failure(exception)
  }

  test("resetHitCounts returns Failure when HitCountService.register fails") {
    setup_ExpectationStore_GetAllExpectations(Right(Map(expectationId1 -> expectation, expectationId2 -> expectation)))
    setup_HitCountService_Delete(Seq(expectationId1))
    setup_HitCountService_Register(Seq(expectationId1), Some(exception))

    expectationService.resetHitCounts(Some(Set(expectationId1))) shouldBe Failure(exception)
  }

  private def setup_ResponseStore_RegisterResponse(
    expectationId: ExpectationId,
    response: Response,
    exceptionOrReturnValue: Either[Exception, DidOverwriteResponse]
  ): Unit = (mockResponseStore.registerResponse _).expects(expectationId, response).respondsWith(exceptionOrReturnValue)

  private def setup_ResponseStore_GetResponses(
    expectationIds: Set[ExpectationId],
    exceptionOrReturnValue: Either[Exception, Map[ExpectationId, Response]]
  ): Unit = (mockResponseStore.getResponses _).expects(expectationIds).respondsWith(exceptionOrReturnValue)

  private def setup_ResponseStore_DeleteResponses(expectationIds: Set[ExpectationId], exception: Option[Exception] = None)
  : Unit = (mockResponseStore.deleteResponses _).expects(expectationIds).respondsWith(exception)

  private def setup_ResponseStore_ClearAllResponses(exception: Option[Exception] = None)
  : Unit = (mockResponseStore.clearAllResponses _).expects().respondsWith(exception)

  private def setup_ExpectationStore_RegisterExpectation(
    expectation: Expectation,
    exceptionOrReturnValue: Either[Exception, ExpectationId]
  ): Unit = (mockExpectationStore.registerExpectation _).expects(expectation).respondsWith(exceptionOrReturnValue)

  private def setup_ExpectationStore_GetIdsForMatchingExpectations(
    request: Request,
    exceptionOrReturnValue: Either[Exception, Set[ExpectationId]]
  ): Unit = (mockExpectationStore.getIdsForMatchingExpectations _).expects(request).respondsWith(exceptionOrReturnValue)

  private def setup_ExpectationStore_GetMostConstrainedExpectationWithId(
    expectationIds: Set[ExpectationId],
    exceptionOrReturnValue: Either[Exception, Option[(ExpectationId, Expectation)]]
  ): Unit = (mockExpectationStore.getMostConstrainedExpectationWithId _).expects(expectationIds).respondsWith(exceptionOrReturnValue)

  private def setup_ExpectationStore_GetAllExpectations(
    exceptionOrReturnValue: Either[Exception, Map[ExpectationId, Expectation]]
  ): Unit = (mockExpectationStore.getAllExpectations _).expects().respondsWith(exceptionOrReturnValue)

  private def setup_ExpectationStore_ClearAllExpectations(exception: Option[Exception] = None)
  : Unit = (mockExpectationStore.clearAllExpectations _).expects().respondsWith(exception)

  private def setup_ExpectationStore_ClearExpectations(ids: Set[ExpectationId], exception: Option[Exception] = None)
  : Unit = (mockExpectationStore.clearExpectations _).expects(ids).respondsWith(exception)

  private def setup_ExpectationsFileService_StoreExpectationsAsJson(
    fileName: String,
    obj: Set[(Expectation, Option[Response])],
    exception: Option[Exception] = None
  ): Unit = (mockExpectationsFileService.storeExpectationsAsJson _).expects(fileName, obj).respondsWith(exception)

  private def setup_ExpectationsFileService_LoadExpectationsFromJson(
    fileName: String,
    exceptionOrReturnValue: Either[Exception, Set[(Expectation, Option[Response])]]
  ): Unit = (mockExpectationsFileService.loadExpectationsFromJson _).expects(fileName).respondsWith(exceptionOrReturnValue)

  private def setup_HitCountService_Register(expectationIds: Seq[ExpectationId], exception: Option[Exception] = None)
  : Unit = (mockHitCountService.register _).expects(expectationIds).respondsWith(exception)

  private def setup_HitCountService_DeleteAll(exception: Option[Exception] = None)
  : Unit = (mockHitCountService.deleteAll _).expects().respondsWith(exception)

  private def setup_HitCountService_Delete(expectationIds: Seq[ExpectationId], exception: Option[Exception] = None)
  : Unit = (mockHitCountService.delete _).expects(expectationIds).respondsWith(exception)

  private def setup_HitCountService_Increment(expectationIds: Seq[ExpectationId], exception: Option[Exception] = None)
  : Unit = (mockHitCountService.increment _).expects(expectationIds).respondsWith(exception)

  def setup_HitCountService_Get(
    expectationIds: Seq[ExpectationId],
    exceptionOrReturnValue: Either[Exception, Map[ExpectationId, Int]]
  ): Unit = (mockHitCountService.get _).expects(expectationIds).respondsWith(exceptionOrReturnValue)
}
