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

  def getAllExpectations: Set[(ExpectationId, ExpectationResponse)]
}

object ExpectationStore {

  case class RegisterExpectationResponseReturnValue(expectationId: ExpectationId, isResponseUpdated: Boolean)

}

@Singleton
class DefaultExpectationStore @Inject()(randomStringGenerator: RandomStringGenerator) extends ExpectationStore {

  //this is the primary store
  private val idToExpectationResponse = TrieMap.empty[ExpectationId, ExpectationResponse]

  //this secondary store is used for faster expectation lookup/matching operations
  private val methodRegistry: MethodRegistry = TrieMap.empty[Method, PathRegistry]

  override def registerExpectationResponse(expectationResponse: ExpectationResponse)
  : RegisterExpectationResponseReturnValue = this.synchronized {

    val (expectation, response) = expectationResponse
    val headerParamRegistry = getHeaderParamRegistry(expectation)
    val expectationId = headerParamRegistry.getOrElseUpdate(expectation.headerParameters, randomStringGenerator.next())
    val oldExpectationAndResponse = idToExpectationResponse.get(expectationId)

    idToExpectationResponse.put(expectationId, expectationResponse)
    headerParamRegistry.put(expectation.headerParameters, expectationId)

    RegisterExpectationResponseReturnValue(
      expectationId,
      isResponseUpdated = oldExpectationAndResponse.exists { case (_, oldResponse) => oldResponse != response }
    )
  }

  override def registerExpectationResponseWithId(expectationResponse: ExpectationResponse, expectationId: ExpectationId)
  : RegisterExpectationResponseReturnValue = this.synchronized {

    val (expectation, response) = expectationResponse
    val headerParamRegistry = getHeaderParamRegistry(expectation)
    val oldExpectationId = headerParamRegistry.get(expectation.headerParameters)
    val oldExpectationAndResponse = oldExpectationId.flatMap(idToExpectationResponse.get)

    headerParamRegistry.put(expectation.headerParameters, expectationId)
    oldExpectationId.foreach(idToExpectationResponse.remove)
    idToExpectationResponse.put(expectationId, expectationResponse)

    RegisterExpectationResponseReturnValue(
      expectationId,
      isResponseUpdated = oldExpectationAndResponse.exists { case (_, oldResponse) => oldResponse != response }
    )
  }

  override def getIdsForMatchingExpectations(request: Request): Set[ExpectationId] = this.synchronized {
    getHeaderParamRegistry(request).collect { // find valid options
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
    methodRegistry.clear()
    idToExpectationResponse.clear()
  }

  override def getAllExpectations: Set[(ExpectationId, ExpectationResponse)] = this.synchronized {
    idToExpectationResponse.toSet
  }

  private def getHeaderParamRegistry(expectationStoreParameters: ExpectationStoreParameters): HeaderParamRegistry = {
    val pathRegistry = methodRegistry.getOrElseUpdate(expectationStoreParameters.method, TrieMap.empty)
    val queryParamRegistry = pathRegistry.getOrElseUpdate(expectationStoreParameters.path, TrieMap.empty)
    val contentRegistry = queryParamRegistry.getOrElseUpdate(expectationStoreParameters.queryParams, TrieMap.empty)
    val headerParamRegistry = contentRegistry.getOrElseUpdate(expectationStoreParameters.content, TrieMap.empty)
    headerParamRegistry
  }
}
