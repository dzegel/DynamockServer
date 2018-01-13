package com.dzegel.DynamockServer.service

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

  test("registerExpectation returns Success when no Exception is thrown") {
    setup_ExpectationStore_RegisterExpectationWithResponse(expectation, response, None)

    expectationService.registerExpectations(Set((expectation, response))) should equal(Success(()))
  }

  test("registerExpectation returns Failure on Exception") {
    val exception = new Exception()
    setup_ExpectationStore_RegisterExpectationWithResponse(expectation, response, Some(exception))

    expectationService.registerExpectations(Set((expectation, response))) should equal(Failure(exception))
  }

  test("getResponse returns Success of response") {
    setup_ExpectationStore_GetResponse(expectation, response = Some(response))

    expectationService.getResponse(request) should equal(Success(Some(response)))
  }

  test("getResponse returns Success of None") {
    setup_ExpectationStore_GetResponse(expectation, response = None)

    expectationService.getResponse(request) should equal(Success(None))
  }

  test("getResponse returns Failure") {
    val exception = new Exception()
    setup_ExpectationStore_GetResponse(expectation, exception = Some(exception))

    expectationService.getResponse(request) should equal(Failure(exception))
  }

  test("clearAllExpectations returns Success") {
    setup_ExpectationStore_ClearAllExpectations()

    expectationService.clearAllExpectations() should equal(Success(()))
  }

  test("clearAllExpectations returns Failure") {
    val exception = new Exception()
    setup_ExpectationStore_ClearAllExpectations(Some(exception))

    expectationService.clearAllExpectations() should equal(Failure(exception))
  }

  test("getAllExpectations returns Success") {
    val expectation = Expectation("dsf", "asd", Map(), null, null)
    val response = Response(200, "some content", Map())
    val expectationResponses = Set(expectation -> response)
    setup_ExpectationStore_GetAllExpectations(Some(expectationResponses))

    expectationService.getAllExpectations should equal(Success(expectationResponses))
  }

  test("getAllExpectations returns Failure") {
    val exception = new Exception("some error message")
    setup_ExpectationStore_GetAllExpectations(exception = Some(exception))

    expectationService.getAllExpectations should equal(Failure(exception))
  }

  test("storeExpectations returns Success") {
    val expectation = Expectation("dsf", "asd", Map(), null, null)
    val response = Response(200, "some content", Map())
    val expectationResponses = Set(expectation -> response)
    val suiteName = "SomeName"

    setup_ExpectationStore_GetAllExpectations(Some(expectationResponses))
    setup_ExpectationsFileService_StoreExpectationsAsJson(suiteName, expectationResponses)

    expectationService.storeExpectations(suiteName) should equal(Success(()))
  }

  test("storeExpectations returns Failure when get all expectations fails") {
    val exception = new Exception("some error message")
    val suiteName = "SomeName"

    setup_ExpectationStore_GetAllExpectations(exception = Some(exception))

    expectationService.storeExpectations(suiteName) should equal(Failure(exception))
  }

  test("storeExpectations returns Failure when store object as json fails") {
    val exception = new Exception("some error message")
    val expectation = Expectation("dsf", "asd", Map(), null, null)
    val response = Response(200, "some content", Map())
    val expectationResponses = Set(expectation -> response)
    val suiteName = "SomeName"

    setup_ExpectationStore_GetAllExpectations(Some(expectationResponses))
    setup_ExpectationsFileService_StoreExpectationsAsJson(suiteName, expectationResponses, Some(exception))

    expectationService.storeExpectations(suiteName) should equal(Failure(exception))
  }

  test("loadExpectations returns Success") {
    val expectation1 = Expectation("1", "1", Map(), null, null)
    val response1 = Response(100, "some content", Map())
    val expectation2 = Expectation("2", "2", Map(), null, null)
    val response2 = Response(200, "some content", Map())
    val expectationResponses = Set(expectation1 -> response1, expectation2 -> response2)
    val suiteName = "SomeName"

    setup_ExpectationsFileService_LoadExpectationsFromJson(suiteName, expectationResponses)
    setup_ExpectationStore_RegisterExpectationWithResponse(expectation1, response1, None)
    setup_ExpectationStore_RegisterExpectationWithResponse(expectation2, response2, None)

    expectationService.loadExpectations(suiteName) should equal(Success(()))
  }

  test("loadExpectations returns Failure when load object from json fails") {
    val exception = new Exception("some error message")
    val expectation1 = Expectation("1", "1", Map(), null, null)
    val response1 = Response(100, "some content", Map())
    val expectation2 = Expectation("2", "2", Map(), null, null)
    val response2 = Response(200, "some content", Map())
    val expectationResponses = Set(expectation1 -> response1, expectation2 -> response2)
    val suiteName = "SomeName"

    setup_ExpectationsFileService_LoadExpectationsFromJson(suiteName, expectationResponses, Some(exception))

    expectationService.loadExpectations(suiteName) should equal(Failure(exception))
  }

  test("loadExpectations returns Failure when register expectation with response fails") {
    val exception = new Exception("some error message")
    val expectation1 = Expectation("1", "1", Map(), null, null)
    val response1 = Response(100, "some content", Map())
    val expectation2 = Expectation("2", "2", Map(), null, null)
    val response2 = Response(200, "some content", Map())
    val expectationResponses = Set(expectation1 -> response1, expectation2 -> response2)
    val suiteName = "SomeName"

    setup_ExpectationsFileService_LoadExpectationsFromJson(suiteName, expectationResponses)
    setup_ExpectationStore_RegisterExpectationWithResponse(expectation1, response1, None)
    setup_ExpectationStore_RegisterExpectationWithResponse(expectation2, response2, Some(exception))

    expectationService.loadExpectations(suiteName) should equal(Failure(exception))
  }

  private def setup_ExpectationStore_RegisterExpectationWithResponse(
    expectation: Expectation,
    response: Response,
    exception: Option[Exception]
  ): Unit = {
    val callHandler = (mockExpectationStore.registerExpectationWithResponse _).expects(expectation, response)
    exception match {
      case None => callHandler.returning(())
      case Some(ex) => callHandler.throwing(ex)
    }
  }

  private def setup_ExpectationStore_GetResponse(
    expectation: Expectation,
    response: Option[Response] = None,
    exception: Option[Exception] = None
  ): Unit = {
    val callHandler = (mockExpectationStore.getResponse _).expects(request)
    exception match {
      case None => callHandler.returning(response)
      case Some(ex) => callHandler.throwing(ex)
    }
  }

  private def setup_ExpectationStore_GetAllExpectations(
    expectationResponses: Option[Set[(Expectation, Response)]] = None,
    exception: Option[Exception] = None
  ): Unit = {
    val callHandler = (mockExpectationStore.getAllExpectations _).expects()
    exception match {
      case None => callHandler.returning(expectationResponses.get)
      case Some(ex) => callHandler.throwing(ex)
    }
  }

  private def setup_ExpectationStore_ClearAllExpectations(exception: Option[Exception] = None): Unit = {
    val callHandler = (mockExpectationStore.clearAllExpectations _).expects()
    exception match {
      case None => callHandler.returning(Unit)
      case Some(ex) => callHandler.throwing(ex)
    }
  }

  private def setup_ExpectationsFileService_StoreExpectationsAsJson(fileName: String, obj: Set[(Expectation, Response)], exception: Option[Exception] = None): Unit = {
    val callHandler = (mockExpectationsFileService.storeExpectationsAsJson _).expects(fileName, obj)
    exception match {
      case None => callHandler.returning(Unit)
      case Some(ex) => callHandler.throwing(ex)
    }
  }

  private def setup_ExpectationsFileService_LoadExpectationsFromJson(fileName: String, returnValue: Set[(Expectation, Response)] = null, exception: Option[Exception] = None): Unit = {
    val callHandler = (mockExpectationsFileService.loadExpectationsFromJson _).expects(fileName)
    exception match {
      case None => callHandler.returning(returnValue)
      case Some(ex) => callHandler.throwing(ex)
    }
  }
}
