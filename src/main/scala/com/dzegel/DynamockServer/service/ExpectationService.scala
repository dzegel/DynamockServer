package com.dzegel.DynamockServer.service

import com.dzegel.DynamockServer.service.ExpectationService._
import com.dzegel.DynamockServer.types._
import com.google.inject.{ImplementedBy, Inject, Singleton}

import scala.util.Try

@ImplementedBy(classOf[DefaultExpectationService])
trait ExpectationService {
  def registerExpectations(expectationResponses: Set[RegisterExpectationsInput]): Try[Set[RegisterExpectationsOutput]]

  def getResponse(request: Request): Try[Option[Response]]

  def clearAllExpectations(): Try[Unit]

  def getAllExpectations: Try[Set[ExpectationResponse]]

  def storeExpectations(suiteName: String): Try[Unit]

  def loadExpectations(suiteName: String): Try[Unit]
}

object ExpectationService {

  case class RegisterExpectationsInput(expectationResponse: ExpectationResponse, clientName: String)

  object RegisterExpectationsInput {
    def apply(expectation: Expectation, response: Response, clientName: String): RegisterExpectationsInput =
      RegisterExpectationsInput(expectation -> response, clientName)
  }

  case class RegisterExpectationsOutput(expectationId: String, clientName: String, didOverwriteResponse: Boolean)

}

@Singleton
class DefaultExpectationService @Inject()(expectationStore: ExpectationStore, fileService: ExpectationsFileService)
  extends ExpectationService {

  override def registerExpectations(expectationResponses: Set[RegisterExpectationsInput])
  : Try[Set[RegisterExpectationsOutput]] = Try {
    this.synchronized {
      expectationResponses.map { x =>
        val output = expectationStore.registerExpectationResponse(x.expectationResponse)
        RegisterExpectationsOutput(output.expectationId, x.clientName, output.isResponseUpdated)
      }
    }
  }

  override def getResponse(request: Request): Try[Option[Response]] = Try {
    this.synchronized {
      val matchingIds = expectationStore.getIdsForMatchingExpectations(request)
      expectationStore.getMostConstrainedExpectationWithId(matchingIds)
    }.map { case (_, (_, response)) => response }
  }

  override def getAllExpectations: Try[Set[ExpectationResponse]] = Try {
    this.synchronized {
      expectationStore.getAllExpectations
    }.map { case (_, expectationResponse) => expectationResponse }
  }

  override def clearAllExpectations(): Try[Unit] = Try {
    this.synchronized {
      expectationStore.clearAllExpectations()
    }
  }

  override def storeExpectations(suiteName: String): Try[Unit] = Try {
    val registeredExpectations = this.synchronized {
      expectationStore.getAllExpectations
    }
    fileService.storeExpectationsAsJson(suiteName, registeredExpectations)
  }

  override def loadExpectations(suiteName: String): Try[Unit] = Try {
    val savedExpectations = fileService.loadExpectationsFromJson(suiteName)
    this.synchronized {
      savedExpectations.foreach { case (expectationId, expectationResponse) =>
        expectationStore.registerExpectationResponseWithId(expectationResponse, expectationId)
      }
    }
  }
}
