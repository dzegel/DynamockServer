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
  hitCountService: HitCountService,
  responseStore: ResponseStore
) extends ExpectationService {

  override def registerExpectations(expectationResponses: Set[RegisterExpectationsInput])
  : Try[Seq[RegisterExpectationsOutput]] = Try {
    this.synchronized {
      val outputs = expectationResponses.toSeq.map { case RegisterExpectationsInput((expectation, response), clientName) =>
        val expectationId = expectationStore.registerExpectation(expectation)
        val didOverwriteResponse = responseStore.registerResponse(expectationId, response)
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
      expectationStore.getMostConstrainedExpectationWithId(matchingIds).flatMap { case (expectationId, _) =>
        responseStore.getResponses(Set(expectationId)).get(expectationId)
      }
    }
  }

  override def getAllExpectations: Try[Set[GetExpectationsOutput]] = Try {
    getAllExpectationResponses.map { case (expectationId, expectation, response) =>
      GetExpectationsOutput(expectationId, expectation, response.get)
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
        val expectationId = expectationStore.registerExpectation(expectation)
        val didOverwriteResponse = optionResponse.exists(responseStore.registerResponse(expectationId, _))
        LoadExpectationsOutput(
          expectationId,
          if (didOverwriteResponse)
            Some(LoadExpectationsOverwriteInfo(expectationId, didOverwriteResponse = true)) //TODO dont need oldExpectationId now that the id is deterministic
          else None)
      }

      val overwrittenIds = outputs.collect {
        case LoadExpectationsOutput(expectationId, Some(LoadExpectationsOverwriteInfo(oldExpectationId, _)))
          if expectationId != oldExpectationId => oldExpectationId
      }
      val writtenIds = outputs.map(output => output.expectationId)
      hitCountService.delete(overwrittenIds)
      hitCountService.register(writtenIds)

      outputs
    }
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
