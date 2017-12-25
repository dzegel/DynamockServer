package com.dzegel.DynamockServer.service

import com.dzegel.DynamockServer.types.{Expectation, Request, Response}
import com.google.inject.{ImplementedBy, Inject, Singleton}

import scala.util.Try

@ImplementedBy(classOf[DefaultExpectationService])
trait ExpectationService {
  def registerExpectation(expectation: Expectation, response: Response): Try[Unit]

  def getResponse(request: Request): Try[Option[Response]]

  def clearAllExpectations(): Try[Unit]

  def getAllExpectations: Try[Set[(Expectation, Response)]]

  def storeExpectations(suiteName: String): Try[Unit]

  def loadExpectations(suiteName: String): Try[Unit]
}

@Singleton
class DefaultExpectationService @Inject()(expectationRegistry: ExpectationRegistry, fileService: ExpectationsFileService) extends ExpectationService {
  override def registerExpectation(expectation: Expectation, response: Response): Try[Unit] =
    Try(expectationRegistry.registerExpectationWithResponse(expectation, response))

  override def getResponse(request: Request): Try[Option[Response]] = Try(expectationRegistry.getResponse(request))

  override def getAllExpectations: Try[Set[(Expectation, Response)]] = Try(expectationRegistry.getAllExpectations)

  override def clearAllExpectations(): Try[Unit] = Try(expectationRegistry.clearAllExpectations())

  override def storeExpectations(suiteName: String): Try[Unit] = Try {
    val registeredExpectations = expectationRegistry.getAllExpectations
    fileService.storeExpectationsAsJson(suiteName, registeredExpectations)
  }

  override def loadExpectations(suiteName: String): Try[Unit] = Try {
    val savedExpectations = fileService.loadExpectationsFromJson(suiteName)
    savedExpectations.foreach {
      case (expectation, response) => expectationRegistry.registerExpectationWithResponse(expectation, response)
    }
  }
}
