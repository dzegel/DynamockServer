package com.dzegel.DynamockServer.service

import com.dzegel.DynamockServer.contract.SetupExpectationPostRequest
import com.google.inject.ImplementedBy

import scala.util.Try

@ImplementedBy(classOf[DefaultSetupService])
trait SetupService {
  def registerExpectation(setupExpectationPostRequest: SetupExpectationPostRequest): Try[Unit]
}

class DefaultSetupService extends SetupService {
  override def registerExpectation(setupExpectationPostRequest: SetupExpectationPostRequest): Try[Unit] = ???
}
