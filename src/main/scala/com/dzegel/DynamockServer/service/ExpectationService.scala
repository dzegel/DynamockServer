package com.dzegel.DynamockServer.service

import com.dzegel.DynamockServer.registry.ExpectationRegistry
import com.dzegel.DynamockServer.contract.{Expectation, Response, ExpectationSetupPostRequest}
import com.google.inject.{ImplementedBy, Inject}

import scala.util.Try

@ImplementedBy(classOf[DefaultExpectationService])
trait ExpectationService {
  def registerExpectation(expectationSetupPostRequest: ExpectationSetupPostRequest): Try[Unit]
}

class DefaultExpectationService @Inject()(expectationRegistry: ExpectationRegistry) extends ExpectationService {
  override def registerExpectation(expectationSetupPostRequest: ExpectationSetupPostRequest): Try[Unit] =
    Try(expectationRegistry
      .registerExpectationWithResponse(expectationSetupPostRequest.expectation, expectationSetupPostRequest.response))
}
