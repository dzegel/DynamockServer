package com.dzegel.DynamockServer.service

import com.dzegel.DynamockServer.service.ExpectationStore._
import com.dzegel.DynamockServer.types._
import com.google.inject.{ImplementedBy, Inject, Singleton}

import scala.collection.concurrent.TrieMap

@ImplementedBy(classOf[DefaultExpectationStore])
trait ExpectationStore {

  def registerExpectationResponse(expectationResponse: ExpectationResponse): RegisterExpectationResponseReturnValue

  def registerExpectationResponseWithId(expectationResponse: ExpectationResponse, id: ExpectationId): RegisterExpectationResponseReturnValue

  def getIdsForMatchingExpectations(request: Request): Set[ExpectationId]

  def getMostConstrainedExpectationWithId(expectationIds: Set[ExpectationId]): Option[(ExpectationId, ExpectationResponse)]

  def clearAllExpectations(): Unit

  def clearExpectations(expectationIds: Set[ExpectationId]): Unit

  def getAllExpectations: Set[(ExpectationId, ExpectationResponse)]
}

object ExpectationStore {

  case class RegisterExpectationResponseReturnValue(expectationId: ExpectationId, isResponseUpdated: Boolean)

  private[service] case class ExpectationKey(method: Method, path: Path, queryParams: QueryParams, content: Content)

  object ExpectationKey {
    implicit def apply(expectation: Expectation): ExpectationKey = ExpectationKey(expectation.method, expectation.path, expectation.queryParams, expectation.content)

    implicit def apply(request: Request): ExpectationKey = ExpectationKey(request.method, request.path, request.queryParams, request.content)
  }

}

@Singleton
class DefaultExpectationStore @Inject()(randomStringGenerator: RandomStringGenerator) extends ExpectationStore {

  private val idToExpectationResponse = TrieMap.empty[ExpectationId, ExpectationResponse]
  private val expectationKeyToHeaderParamRegistry = TrieMap.empty[ExpectationKey, HeaderParamRegistry]

  override def registerExpectationResponse(expectationResponse: ExpectationResponse)
  : RegisterExpectationResponseReturnValue = this.synchronized {

    val (expectation, response) = expectationResponse
    val headerParamRegistry = expectationKeyToHeaderParamRegistry.getOrElseUpdate(expectation, TrieMap.empty)
    val expectationId = headerParamRegistry.getOrElseUpdate(expectation.headerParameters, randomStringGenerator.next())
    val oldExpectationResponse = idToExpectationResponse.get(expectationId)

    idToExpectationResponse.put(expectationId, expectationResponse)
    headerParamRegistry.put(expectation.headerParameters, expectationId)

    RegisterExpectationResponseReturnValue(
      expectationId,
      isResponseUpdated = oldExpectationResponse.exists { case (_, oldResponse) => oldResponse != response }
    )
  }

  override def registerExpectationResponseWithId(expectationResponse: ExpectationResponse, expectationId: ExpectationId)
  : RegisterExpectationResponseReturnValue = this.synchronized {

    val (expectation, response) = expectationResponse
    val headerParamRegistry = expectationKeyToHeaderParamRegistry.getOrElseUpdate(expectation, TrieMap.empty)
    val oldExpectationId = headerParamRegistry.get(expectation.headerParameters)
    val oldExpectationResponse = oldExpectationId.flatMap(idToExpectationResponse.get)

    headerParamRegistry.put(expectation.headerParameters, expectationId)
    oldExpectationId.foreach(idToExpectationResponse.remove)
    idToExpectationResponse.put(expectationId, expectationResponse)

    RegisterExpectationResponseReturnValue(
      expectationId,
      isResponseUpdated = oldExpectationResponse.exists { case (_, oldResponse) => oldResponse != response }
    )
  }

  override def getIdsForMatchingExpectations(request: Request): Set[ExpectationId] = this.synchronized {
    expectationKeyToHeaderParamRegistry.getOrElse(request, TrieMap.empty).collect { // find valid options
      case (HeaderParameters(included, excluded), expectationId)
        if included.subsetOf(request.headers) && excluded.intersect(request.headers).isEmpty => expectationId
    }.toSet
  }

  override def getMostConstrainedExpectationWithId(expectationIds: Set[ExpectationId])
  : Option[(ExpectationId, ExpectationResponse)] = this.synchronized {
    expectationIds.map(id => (id, idToExpectationResponse(id))).reduceOption[(ExpectationId, ExpectationResponse)] {
      case (left, right) =>
        val (_, (Expectation(_, _, _, leftHeaderParameters, _), _)) = left
        val HeaderParameters(leftIncluded, leftExcluded) = leftHeaderParameters
        val (_, (Expectation(_, _, _, rightHeaderParameters, _), _)) = right
        val HeaderParameters(rightIncluded, rightExcluded) = rightHeaderParameters

        val leftConstraintSize = leftIncluded.size + leftExcluded.size
        val rightConstraintSize = rightIncluded.size + rightExcluded.size

        if (leftConstraintSize == rightConstraintSize) //if they have the same number of constraints pick either one deterministically
          if (leftHeaderParameters.hashCode() <= rightHeaderParameters.hashCode()) left else right
        else if (leftConstraintSize > rightConstraintSize) left else right //find the option with the most constraints
    }
  }

  override def clearAllExpectations(): Unit = this.synchronized {
    expectationKeyToHeaderParamRegistry.clear()
    idToExpectationResponse.clear()
  }

  override def clearExpectations(expectationIds: Set[ExpectationId]): Unit = this.synchronized {
    expectationIds.foreach { id =>
      idToExpectationResponse.remove(id).foreach {
        case (expectation, _) =>
          val headerParamRegistry = expectationKeyToHeaderParamRegistry.getOrElse(expectation, TrieMap.empty)
          headerParamRegistry.remove(expectation.headerParameters)
          if (headerParamRegistry.isEmpty) {
            expectationKeyToHeaderParamRegistry.remove(expectation)
          }
      }
    }
  }

  override def getAllExpectations: Set[(ExpectationId, ExpectationResponse)] = this.synchronized {
    idToExpectationResponse.toSet
  }
}
