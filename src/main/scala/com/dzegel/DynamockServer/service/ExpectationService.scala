package com.dzegel.DynamockServer.service

import com.dzegel.DynamockServer.service.ExpectationService._
import com.dzegel.DynamockServer.types._
import com.google.inject.{ImplementedBy, Inject, Singleton}

import scala.util.Try

@ImplementedBy(classOf[DefaultExpectationService])
trait ExpectationService {
  def registerExpectations(expectationResponses: Set[RegisterExpectationsInput]): Try[Seq[RegisterExpectationsOutput]]

  def getResponse(request: Request): Try[Option[Response]]

  def clearExpectations(expectationIds: Option[Set[ExpectationId]]): Try[Unit]

  def getAllExpectations: Try[Set[GetExpectationsOutput]]

  def storeExpectations(suiteName: String): Try[Unit]

  def loadExpectations(suiteName: String): Try[Seq[LoadExpectationsOutput]]
}

object ExpectationService {

  case class RegisterExpectationsInput(expectationResponse: ExpectationResponse, clientName: String)

  object RegisterExpectationsInput {
    def apply(expectation: Expectation, response: Response, clientName: String): RegisterExpectationsInput =
      RegisterExpectationsInput(expectation -> response, clientName)
  }

  case class RegisterExpectationsOutput(expectationId: ExpectationId, clientName: String, didOverwriteResponse: Boolean)

  case class GetExpectationsOutput(expectationId: ExpectationId, expectation: Expectation, response: Response)

  case class LoadExpectationsOverwriteInfo(oldExpectationId: ExpectationId, didOverwriteResponse: Boolean)

  case class LoadExpectationsOutput(expectationId: ExpectationId, overwriteInfo: Option[LoadExpectationsOverwriteInfo])

}

@Singleton
class DefaultExpectationService @Inject()(
  expectationStore: ExpectationStore,
  fileService: ExpectationsFileService,
  hitCountService: HitCountService
) extends ExpectationService {

  override def registerExpectations(expectationResponses: Set[RegisterExpectationsInput])
  : Try[Seq[RegisterExpectationsOutput]] = Try {
    this.synchronized {
      val outputs = expectationResponses.toSeq.map { x =>
        val output = expectationStore.registerExpectationResponse(x.expectationResponse)
        RegisterExpectationsOutput(output.expectationId, x.clientName, output.isResponseUpdated)
      }
      hitCountService.register(outputs.map(output => output.expectationId))
      outputs
    }
  }

  override def getResponse(request: Request): Try[Option[Response]] = Try {
    this.synchronized {
      val matchingIds = expectationStore.getIdsForMatchingExpectations(request)
      hitCountService.increment(matchingIds.toSeq)
      expectationStore.getMostConstrainedExpectationWithId(matchingIds)
    }.map { case (_, (_, response)) => response }
  }

  override def getAllExpectations: Try[Set[GetExpectationsOutput]] = Try {
    this.synchronized {
      expectationStore.getAllExpectations
    }.map { case (id, (expectation, response)) => GetExpectationsOutput(id, expectation, response) }
  }

  override def clearExpectations(expectationIds: Option[Set[ExpectationId]]): Try[Unit] = Try {
    this.synchronized {
      expectationIds match {
        case None =>
          expectationStore.clearAllExpectations()
          hitCountService.deleteAll()
        case Some(ids) =>
          expectationStore.clearExpectations(ids)
          hitCountService.delete(ids.toSeq)
      }
    }
  }

  override def storeExpectations(suiteName: String): Try[Unit] = Try {
    val registeredExpectations = this.synchronized {
      expectationStore.getAllExpectations
    }
    fileService.storeExpectationsAsJson(suiteName, registeredExpectations)
  }

  override def loadExpectations(suiteName: String): Try[Seq[LoadExpectationsOutput]] = Try {
    val savedExpectations = fileService.loadExpectationsFromJson(suiteName)
    this.synchronized {
      val outputs = savedExpectations.toSeq.map { case (expectationId, expectationResponse) =>
        val output = expectationStore.registerExpectationResponseWithId(expectationResponse, expectationId)
        LoadExpectationsOutput(expectationId, output.map(x => LoadExpectationsOverwriteInfo(x.oldExpectationId, x.isResponseUpdated)))
      }

      val overwrittenIds = outputs.flatMap(output => output.overwriteInfo.map(info => info.oldExpectationId))
      val writtenIds = outputs.map(output => output.expectationId)
      hitCountService.delete(overwrittenIds)
      hitCountService.register(writtenIds)

      outputs
    }
  }
}
