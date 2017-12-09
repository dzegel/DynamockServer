package com.dzegel.DynamockServer.service

import com.dzegel.DynamockServer.registry.ExpectationRegistry
import com.dzegel.DynamockServer.types._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSuite, Matchers}

import scala.util.{Failure, Success}

class ExpectationServiceTests extends FunSuite with MockFactory with Matchers {

  private val mockExpectationRegistry = mock[ExpectationRegistry]
  private val mockExpectationsFileService = mock[ExpectationsFileService]
  private val expectationService = new DefaultExpectationService(mockExpectationRegistry, mockExpectationsFileService)

  private val expectation = Expectation("POST", "somePath", Map.empty, HeaderParameters(Set.empty, Set.empty), Content(""))
  private val request = Request(expectation.method, expectation.path, expectation.queryParams, expectation.headerParameters.included, expectation.content)
  private val response = Response(200, "", Map.empty)

  test("registerExpectation returns Success when no Exception is thrown") {
    setup_ExpectationRegistry_RegisterExpectationWithResponse(expectation, response, None)

    expectationService.registerExpectation(expectation, response) should equal(Success(()))
  }

  test("registerExpectation returns Failure on Exception") {
    val exception = new Exception()
    setup_ExpectationRegistry_RegisterExpectationWithResponse(expectation, response, Some(exception))

    expectationService.registerExpectation(expectation, response) should equal(Failure(exception))
  }

  test("getResponse returns Success of response") {
    setup_ExpectationRegistry_GetResponse(expectation, response = Some(response))

    expectationService.getResponse(request) should equal(Success(Some(response)))
  }

  test("getResponse returns Success of None") {
    setup_ExpectationRegistry_GetResponse(expectation, response = None)

    expectationService.getResponse(request) should equal(Success(None))
  }

  test("getResponse returns Failure") {
    val exception = new Exception()
    setup_ExpectationRegistry_GetResponse(expectation, exception = Some(exception))

    expectationService.getResponse(request) should equal(Failure(exception))
  }

  test("clearAllExpectations returns Success") {
    setup_ExpectationRegistry_ClearAllExpectations()

    expectationService.clearAllExpectations() should equal(Success(()))
  }

  test("clearAllExpectations returns Failure") {
    val exception = new Exception()
    setup_ExpectationRegistry_ClearAllExpectations(Some(exception))

    expectationService.clearAllExpectations() should equal(Failure(exception))
  }

  test("getAllExpectations returns Success") {
    val expectation = Expectation("dsf", "asd", Map(), null, null)
    val response = Response(200, "some content", Map())
    val pairs = Set(expectation -> response)
    setup_ExpectationRegistry_GetAllExpectations(Some(pairs))

    expectationService.getAllExpectations should equal(Success(pairs))
  }

  test("getAllExpectations returns Failure") {
    val exception = new Exception("some error message")
    setup_ExpectationRegistry_GetAllExpectations(exception = Some(exception))

    expectationService.getAllExpectations should equal(Failure(exception))
  }

  test("storeExpectations returns Success") {
    val expectation = Expectation("dsf", "asd", Map(), null, null)
    val response = Response(200, "some content", Map())
    val pairs = Set(expectation -> response)
    val suiteName = "SomeName"

    setup_ExpectationRegistry_GetAllExpectations(Some(pairs))
    setup_ExpectationsFileService_StoreExpectationsAsJson(s"$suiteName.expectations", pairs)

    expectationService.storeExpectations(suiteName) should equal(Success(()))
  }

  test("storeExpectations returns Failure when get all expectations fails") {
    val exception = new Exception("some error message")
    val suiteName = "SomeName"

    setup_ExpectationRegistry_GetAllExpectations(exception = Some(exception))

    expectationService.storeExpectations(suiteName) should equal(Failure(exception))
  }

  test("storeExpectations returns Failure when store object as json fails") {
    val exception = new Exception("some error message")
    val expectation = Expectation("dsf", "asd", Map(), null, null)
    val response = Response(200, "some content", Map())
    val pairs = Set(expectation -> response)
    val suiteName = "SomeName"

    setup_ExpectationRegistry_GetAllExpectations(Some(pairs))
    setup_ExpectationsFileService_StoreExpectationsAsJson(s"$suiteName.expectations", pairs, Some(exception))

    expectationService.storeExpectations(suiteName) should equal(Failure(exception))
  }

  test("loadExpectations returns Success") {
    val expectation1 = Expectation("1", "1", Map(), null, null)
    val response1 = Response(100, "some content", Map())
    val expectation2 = Expectation("2", "2", Map(), null, null)
    val response2 = Response(200, "some content", Map())
    val pairs = Set(expectation1 -> response1, expectation2 -> response2)
    val suiteName = "SomeName"

    setup_ExpectationsFileService_LoadExpectationsFromJson(s"$suiteName.expectations", pairs)
    setup_ExpectationRegistry_RegisterExpectationWithResponse(expectation1, response1, None)
    setup_ExpectationRegistry_RegisterExpectationWithResponse(expectation2, response2, None)

    expectationService.loadExpectations(suiteName) should equal(Success(()))
  }

  test("loadExpectations returns Failure when load object from json fails") {
    val exception = new Exception("some error message")
    val expectation1 = Expectation("1", "1", Map(), null, null)
    val response1 = Response(100, "some content", Map())
    val expectation2 = Expectation("2", "2", Map(), null, null)
    val response2 = Response(200, "some content", Map())
    val pairs = Set(expectation1 -> response1, expectation2 -> response2)
    val suiteName = "SomeName"

    setup_ExpectationsFileService_LoadExpectationsFromJson(s"$suiteName.expectations", pairs, Some(exception))

    expectationService.loadExpectations(suiteName) should equal(Failure(exception))
  }

  test("loadExpectations returns Failure when register expectation with response fails") {
    val exception = new Exception("some error message")
    val expectation1 = Expectation("1", "1", Map(), null, null)
    val response1 = Response(100, "some content", Map())
    val expectation2 = Expectation("2", "2", Map(), null, null)
    val response2 = Response(200, "some content", Map())
    val pairs = Set(expectation1 -> response1, expectation2 -> response2)
    val suiteName = "SomeName"

    setup_ExpectationsFileService_LoadExpectationsFromJson(s"$suiteName.expectations", pairs)
    setup_ExpectationRegistry_RegisterExpectationWithResponse(expectation1, response1, None)
    setup_ExpectationRegistry_RegisterExpectationWithResponse(expectation2, response2, Some(exception))

    expectationService.loadExpectations(suiteName) should equal(Failure(exception))
  }

  private def setup_ExpectationRegistry_RegisterExpectationWithResponse(
    expectation: Expectation,
    response: Response,
    exception: Option[Exception]
  ): Unit = {
    val callHandler = (mockExpectationRegistry.registerExpectationWithResponse _).expects(expectation, response)
    exception match {
      case None => callHandler.returning(())
      case Some(ex) => callHandler.throwing(ex)
    }
  }

  private def setup_ExpectationRegistry_GetResponse(
    expectation: Expectation,
    response: Option[Response] = None,
    exception: Option[Exception] = None
  ): Unit = {
    val callHandler = (mockExpectationRegistry.getResponse _).expects(request)
    exception match {
      case None => callHandler.returning(response)
      case Some(ex) => callHandler.throwing(ex)
    }
  }

  private def setup_ExpectationRegistry_GetAllExpectations(
    expectationAndResponsePairs: Option[Set[(Expectation, Response)]] = None,
    exception: Option[Exception] = None
  ): Unit = {
    val callHandler = (mockExpectationRegistry.getAllExpectations _).expects()
    exception match {
      case None => callHandler.returning(expectationAndResponsePairs.get)
      case Some(ex) => callHandler.throwing(ex)
    }
  }

  private def setup_ExpectationRegistry_ClearAllExpectations(exception: Option[Exception] = None): Unit = {
    val callHandler = (mockExpectationRegistry.clearAllExpectations _).expects()
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
