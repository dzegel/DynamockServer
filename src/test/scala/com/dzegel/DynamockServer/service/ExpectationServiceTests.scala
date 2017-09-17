package com.dzegel.DynamockServer.service

import com.dzegel.DynamockServer.registry.ExpectationRegistry
import com.dzegel.DynamockServer.contract.{Expectation, Response, ExpectationSetupPostRequest}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSuite, Matchers}

import scala.util.{Failure, Success}

class ExpectationServiceTests extends FunSuite with MockFactory with Matchers {

  private val mockExpectationRegistry = mock[ExpectationRegistry]
  private val ExpectationService = new DefaultExpectationService(mockExpectationRegistry)

  private val expectation = Expectation("", "", "")
  private val response = Response()
  val expectationSetupPostRequest = ExpectationSetupPostRequest(expectation, response)

  test("registerExpectation returns Success when no Exception is thrown") {
    setup_ExpectationRegistry_RegisterExpectationWithResponse(expectation, response, None)

    ExpectationService.registerExpectation(expectationSetupPostRequest) should equal(Success(()))
  }

  test("registerExpectation returns Failure on Exception") {
    val exception = new Exception()
    setup_ExpectationRegistry_RegisterExpectationWithResponse(expectation, response, Some(exception))

    ExpectationService.registerExpectation(expectationSetupPostRequest) should equal(Failure(exception))
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
}
