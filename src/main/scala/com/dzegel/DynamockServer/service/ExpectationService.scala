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

  def getHitCounts(expectationIds: Set[ExpectationId]): Try[Map[ExpectationId, Int]]

  def resetHitCounts(expectationIds: Option[Set[ExpectationId]]): Try[Unit]
}

object ExpectationService {

  case class RegisterExpectationsInput(expectation: Expectation, response: Option[Response], clientName: String)

  case class RegisterExpectationsOutput(expectationId: ExpectationId, clientName: String, didOverwriteResponse: DidOverwriteResponse)

  case class GetExpectationsOutput(expectationId: ExpectationId, expectation: Expectation, response: Option[Response])

  case class LoadExpectationsOutput(expectationId: ExpectationId, didOverwriteResponse: DidOverwriteResponse)

}

@Singleton
class DefaultExpectationService @Inject()(
  expectationStore: ExpectationStore,
  fileService: ExpectationsFileService,
  hitCountService: HitCountService,
  responseStore: ResponseStore
) extends ExpectationService {

  override def registerExpectations(expectationResponses: Set[RegisterExpectationsInput])
  : Try[Seq[RegisterExpectationsOutput]] = Try {
    this.synchronized {
      val outputs = expectationResponses.toSeq.map { case RegisterExpectationsInput(expectation, optionResponse, clientName) =>
        val (expectationId, didOverwriteResponse) = registerExpectation(expectation, optionResponse)
        RegisterExpectationsOutput(expectationId, clientName, didOverwriteResponse)
      }
      hitCountService.register(outputs.map(output => output.expectationId))
      outputs
    }
  }

  override def getResponse(request: Request): Try[Option[Response]] = Try {
    this.synchronized {
      val matchingIds = expectationStore.getIdsForMatchingExpectations(request)
      hitCountService.increment(matchingIds.toSeq)
      val expectationIdToResponse = responseStore.getResponses(matchingIds)
      expectationStore.getMostConstrainedExpectationWithId(expectationIdToResponse.keySet)
        .flatMap { case (expectationId, _) => expectationIdToResponse.get(expectationId) }
    }
  }

  override def getAllExpectations: Try[Set[GetExpectationsOutput]] = Try {
    getAllExpectationResponses.map { case (expectationId, expectation, response) =>
      GetExpectationsOutput(expectationId, expectation, response)
    }
  }

  override def clearExpectations(expectationIds: Option[Set[ExpectationId]]): Try[Unit] = Try {
    this.synchronized {
      expectationIds match {
        case None =>
          expectationStore.clearAllExpectations()
          responseStore.clearAllResponses()
          hitCountService.deleteAll()
        case Some(ids) =>
          expectationStore.clearExpectations(ids)
          responseStore.deleteResponses(ids)
          hitCountService.delete(ids.toSeq)
      }
    }
  }

  override def storeExpectations(suiteName: String): Try[Unit] = Try {
    fileService.storeExpectationsAsJson(suiteName, getAllExpectationResponses.map { case (_, expectation, response) =>
      (expectation, response)
    })
  }

  private def getAllExpectationResponses: Set[(ExpectationId, Expectation, Option[Response])] = {
    val (expectationIdToExpectation, expectationIdToResponse) = this.synchronized {
      val expectationIdToExpectation = expectationStore.getAllExpectations
      val expectationIdToResponse = responseStore.getResponses(expectationIdToExpectation.keySet)
      (expectationIdToExpectation, expectationIdToResponse)
    }
    expectationIdToExpectation.map { case (expectationId, expectation) =>
      (expectationId, expectation, expectationIdToResponse.get(expectationId))
    }.toSet
  }

  override def loadExpectations(suiteName: String): Try[Seq[LoadExpectationsOutput]] = Try {
    val savedExpectations = fileService.loadExpectationsFromJson(suiteName)
    this.synchronized {
      val outputs = savedExpectations.toSeq.map { case (expectation, optionResponse) =>
        val (expectationId, didOverwriteResponse) = registerExpectation(expectation, optionResponse)
        LoadExpectationsOutput(expectationId, didOverwriteResponse)
      }
      hitCountService.register(outputs.map(output => output.expectationId))
      outputs
    }
  }

  private def registerExpectation(expectation: Expectation, optionResponse: Option[Response]): (ExpectationId, DidOverwriteResponse) = {
    val expectationId = expectationStore.registerExpectation(expectation)
    val didOverwriteResponse = optionResponse match {
      case Some(response) => responseStore.registerResponse(expectationId, response)
      case None =>
        val exists = responseStore.getResponses(Set(expectationId)).contains(expectationId)
        responseStore.deleteResponses(Set(expectationId))
        exists
    }
    (expectationId, didOverwriteResponse)
  }

  override def getHitCounts(expectationIds: Set[ExpectationId]) = Try {
    hitCountService.get(expectationIds.toSeq)
  }

  override def resetHitCounts(expectationIds: Option[Set[ExpectationId]]) = Try {
    this.synchronized {
      val allRegisteredIds = expectationStore.getAllExpectations.map {
        case (expectationId, _) => expectationId
      }.toSet
      val idsToReset = expectationIds match {
        case None => allRegisteredIds.toSeq
        case Some(ids) => allRegisteredIds.intersect(ids).toSeq
      }
      hitCountService.delete(idsToReset)
      hitCountService.register(idsToReset)
    }
  }
}
