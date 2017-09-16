package com.dzegel.DynamockServer.service

import com.dzegel.DynamockServer.registry.{Expectation, Response, SetupRegistry}
import com.dzegel.DynamockServer.contract.SetupExpectationPostRequest
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSuite, Matchers}

import scala.util.{Failure, Success}

class SetupServiceTests extends FunSuite with MockFactory with Matchers {

  private val mockSetupRegistry = mock[SetupRegistry]
  private val setupService = new DefaultSetupService(mockSetupRegistry)

  private val expectation = Expectation("", "", "")
  private val response = Response()
  val setupExpectationPostRequest = SetupExpectationPostRequest(expectation, response)

  test("registerExpectation returns Success when no Exception is thrown") {
    setupSetupRegistryRegisterExpectationWithResponse(expectation, response, None)

    setupService.registerExpectation(setupExpectationPostRequest) should equal(Success(()))
  }

  test("registerExpectation returns Failure on Exception") {
    val exception = new Exception()
    setupSetupRegistryRegisterExpectationWithResponse(expectation, response, Some(exception))

    setupService.registerExpectation(setupExpectationPostRequest) should equal(Failure(exception))
  }

  private def setupSetupRegistryRegisterExpectationWithResponse(
    expectation: Expectation,
    response: Response,
    exception: Option[Exception]
  ): Unit = {
    val callHandler = (mockSetupRegistry.registerExpectationWithResponse _).expects(expectation, response)
    exception match {
      case None => callHandler.returning(())
      case Some(ex) => callHandler.throwing(ex)
    }
  }
}
