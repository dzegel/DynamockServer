package com.dzegel.DynamockServer.service

import com.dzegel.DynamockServer.types.{Expectation, Request, Response}
import com.google.inject.{ImplementedBy, Inject, Singleton}

import scala.util.Try

@ImplementedBy(classOf[DefaultExpectationService])
trait ExpectationService {
  def registerExpectations(expectationResponses: Set[(Expectation, Response)]): Try[Unit]

  def getResponse(request: Request): Try[Option[Response]]

  def clearAllExpectations(): Try[Unit]

  def getAllExpectations: Try[Set[(Expectation, Response)]]

  def storeExpectations(suiteName: String): Try[Unit]

  def loadExpectations(suiteName: String): Try[Unit]
}

@Singleton
class DefaultExpectationService @Inject()(expectationStore: ExpectationStore, fileService: ExpectationsFileService) extends ExpectationService {
  override def registerExpectations(expectationResponses: Set[(Expectation, Response)]): Try[Unit] =
    Try(expectationResponses.foreach {
      case (expectation, response) => expectationStore.registerExpectationWithResponse(expectation, response)
    })

  override def getResponse(request: Request): Try[Option[Response]] = Try(expectationStore.getResponse(request))

  override def getAllExpectations: Try[Set[(Expectation, Response)]] = Try(expectationStore.getAllExpectations)

  override def clearAllExpectations(): Try[Unit] = Try(expectationStore.clearAllExpectations())

  override def storeExpectations(suiteName: String): Try[Unit] = Try {
    val registeredExpectations = expectationStore.getAllExpectations
    fileService.storeExpectationsAsJson(suiteName, registeredExpectations)
  }

  override def loadExpectations(suiteName: String): Try[Unit] = Try {
    val savedExpectations = fileService.loadExpectationsFromJson(suiteName)
    savedExpectations.foreach {
      case (expectation, response) => expectationStore.registerExpectationWithResponse(expectation, response)
    }
  }
}
