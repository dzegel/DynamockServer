package com.dzegel.DynamockServer.service

import com.dzegel.DynamockServer.registry.ExpectationRegistry
import com.dzegel.DynamockServer.types.{Expectation, Response}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSuite, Matchers}

import scala.util.{Failure, Success}

class ExpectationServiceTests extends FunSuite with MockFactory with Matchers {

  private val mockExpectationRegistry = mock[ExpectationRegistry]
  private val expectationService = new DefaultExpectationService(mockExpectationRegistry)

  private val expectation = Expectation("", "", "")
  private val response = Response(200)

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

    expectationService.getResponse(expectation) should equal(Success(Some(response)))
  }

  test("getResponse returns Success of None") {
    setup_ExpectationRegistry_GetResponse(expectation, response = None)

    expectationService.getResponse(expectation) should equal(Success(None))
  }

  test("getResponse returns Failure") {
    val exception = new Exception()
    setup_ExpectationRegistry_GetResponse(expectation, exception = Some(exception))

    expectationService.getResponse(expectation) should equal(Failure(exception))
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
    val callHandler = (mockExpectationRegistry.getResponse _).expects(expectation)
    exception match {
      case None => callHandler.returning(response)
      case Some(ex) => callHandler.throwing(ex)
    }
  }
}
