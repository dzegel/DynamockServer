package com.dzegel.DynamockServer.service

import com.dzegel.DynamockServer.registry.SetupRegistry
import com.dzegel.DynamockServer.contract.SetupExpectationPostRequest
import com.google.inject.{ImplementedBy, Inject}

import scala.util.Try

@ImplementedBy(classOf[DefaultSetupService])
trait SetupService {
  def registerExpectation(setupExpectationPostRequest: SetupExpectationPostRequest): Try[Unit]
}

class DefaultSetupService @Inject()(setupRegistry: SetupRegistry) extends SetupService {
  override def registerExpectation(setupExpectationPostRequest: SetupExpectationPostRequest): Try[Unit] =
    Try(setupRegistry
      .registerExpectationWithResponse(setupExpectationPostRequest.expectation, setupExpectationPostRequest.response))
}
