package com.dzegel.DynamockServer.service

import com.dzegel.DynamockServer.service.ExpectationStore._
import com.dzegel.DynamockServer.types._
import com.google.inject.{ImplementedBy, Singleton}

import scala.collection.concurrent.TrieMap
import scala.language.implicitConversions

@ImplementedBy(classOf[DefaultExpectationStore])
trait ExpectationStore {

  def registerExpectation(expectation: Expectation): ExpectationId

  def getIdsForMatchingExpectations(request: Request): Set[ExpectationId]

  def getMostConstrainedExpectationWithId(expectationIds: Set[ExpectationId]): Option[(ExpectationId, Expectation)]

  def clearAllExpectations(): Unit

  def clearExpectations(expectationIds: Set[ExpectationId]): Unit

  def getAllExpectations: Map[ExpectationId, Expectation]
}

private [service] object ExpectationStore {

  case class ExpectationKey(method: Method, path: Path, queryParams: QueryParams, content: Content)

  object ExpectationKey {
    implicit def apply(expectation: Expectation): ExpectationKey = ExpectationKey(expectation.method, expectation.path, expectation.queryParams, expectation.content)

    implicit def apply(request: Request): ExpectationKey = ExpectationKey(request.method, request.path, request.queryParams, request.content)
  }

}

@Singleton
class DefaultExpectationStore extends ExpectationStore {

  private val idToExpectation = TrieMap.empty[ExpectationId, Expectation]
  private val expectationKeyToHeaderParamRegistry = TrieMap.empty[ExpectationKey, HeaderParamRegistry]

  override def registerExpectation(expectation: Expectation): ExpectationId = this.synchronized {
    val headerParamRegistry = expectationKeyToHeaderParamRegistry.getOrElseUpdate(expectation, TrieMap.empty)
    val expectationId = headerParamRegistry.getOrElseUpdate(expectation.headerParameters, getExpectationId(expectation))

    idToExpectation.put(expectationId, expectation)
    headerParamRegistry.put(expectation.headerParameters, expectationId)

    expectationId
  }

  private def getExpectationId(expectation: Expectation): String = expectation.hashCode().toString

  override def getIdsForMatchingExpectations(request: Request): Set[ExpectationId] = this.synchronized {
    expectationKeyToHeaderParamRegistry.getOrElse(request, TrieMap.empty).collect { // find valid options
      case (HeaderParameters(included, excluded), expectationId)
        if included.subsetOf(request.headers) && excluded.intersect(request.headers).isEmpty => expectationId
    }.toSet
  }

  override def getMostConstrainedExpectationWithId(expectationIds: Set[ExpectationId])
  : Option[(ExpectationId, Expectation)] = this.synchronized {
    expectationIds.map(id => (id, idToExpectation(id))).reduceOption[(ExpectationId, Expectation)] {
      case (left, right) =>
        val (_, Expectation(_, _, _, leftHeaderParameters, _)) = left
        val HeaderParameters(leftIncluded, leftExcluded) = leftHeaderParameters
        val (_, Expectation(_, _, _, rightHeaderParameters, _)) = right
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
    idToExpectation.clear()
  }

  override def clearExpectations(expectationIds: Set[ExpectationId]): Unit = this.synchronized {
    expectationIds.foreach { id =>
      idToExpectation.remove(id).foreach { expectation =>
        val headerParamRegistry = expectationKeyToHeaderParamRegistry.getOrElse(expectation, TrieMap.empty)
        headerParamRegistry.remove(expectation.headerParameters)
        if (headerParamRegistry.isEmpty) {
          expectationKeyToHeaderParamRegistry.remove(expectation)
        }
      }
    }
  }

  override def getAllExpectations: Map[ExpectationId, Expectation] = this.synchronized {
    idToExpectation.toMap
  }
}
