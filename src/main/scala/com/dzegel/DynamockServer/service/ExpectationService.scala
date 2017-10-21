package com.dzegel.DynamockServer.service

import com.dzegel.DynamockServer.registry.ExpectationRegistry
import com.dzegel.DynamockServer.types.{Expectation, Response}
import com.google.inject.{ImplementedBy, Inject}

import scala.util.Try

@ImplementedBy(classOf[DefaultExpectationService])
trait ExpectationService {
  def registerExpectation(expectation: Expectation, response: Response): Try[Unit]

  def getResponse(expectation: Expectation): Try[Option[Response]]
}

class DefaultExpectationService @Inject()(expectationRegistry: ExpectationRegistry) extends ExpectationService {
  override def registerExpectation(expectation: Expectation, response: Response): Try[Unit] =
    Try(expectationRegistry.registerExpectationWithResponse(expectation, response))

  override def getResponse(expectation: Expectation): Try[Option[Response]] =
    Try(expectationRegistry.getResponse(expectation))
}
