package com.dzegel.DynamockServer.service

import com.dzegel.DynamockServer.types._
import com.google.inject.{ImplementedBy, Singleton}

import scala.collection.concurrent.TrieMap

@ImplementedBy(classOf[DefaultExpectationStore])
trait ExpectationStore {
  def registerExpectationWithResponse(expectation: Expectation, response: Response): Unit

  def getResponse(request: Request): Option[Response]

  def clearAllExpectations(): Unit

  def getAllExpectations: Set[(Expectation, Response)]
}

@Singleton
class DefaultExpectationStore extends ExpectationStore {

  private val methodRegistry: MethodRegistry = TrieMap.empty[Method, PathRegistry]

  override def registerExpectationWithResponse(expectation: Expectation, response: Response): Unit =
    getHeaderParamRegistry(expectation).put(expectation.headerParameters, response)

  override def getResponse(request: Request): Option[Response] = {
    getHeaderParamRegistry(request).filter { // find valid options
      case (HeaderParameters(included, excluded), _) =>
        included.subsetOf(request.headers) && excluded.intersect(request.headers).isEmpty
    }.reduceOption[(HeaderParameters, Response)] {
      case (left, right) =>
        val (leftHeaderParameters, _) = left
        val HeaderParameters(leftIncluded, leftExcluded) = leftHeaderParameters
        val (rightHeaderParameters, _) = right
        val HeaderParameters(rightIncluded, rightExcluded) = rightHeaderParameters

        val leftConstraintSize = leftIncluded.size + leftExcluded.size
        val rightConstraintSize = rightIncluded.size + rightExcluded.size

        if (leftConstraintSize == rightConstraintSize) //if they have the same number of constraints pick one deterministically
          if (leftHeaderParameters.hashCode() <= rightHeaderParameters.hashCode()) left else right
        else
          if (leftConstraintSize > rightConstraintSize) left else right //find the option with the most constraints
    }.map { case (_, response) => response }
  }

  private def getHeaderParamRegistry(expectationStoreParameters: ExpectationStoreParameters): HeaderParamRegistry = {
    val pathRegistry = methodRegistry.getOrElseUpdate(expectationStoreParameters.method, TrieMap.empty)
    val queryParamRegistry = pathRegistry.getOrElseUpdate(expectationStoreParameters.path, TrieMap.empty)
    val contentRegistry = queryParamRegistry.getOrElseUpdate(expectationStoreParameters.queryParams, TrieMap.empty)
    val headerParamRegistry = contentRegistry.getOrElseUpdate(expectationStoreParameters.content, TrieMap.empty)
    headerParamRegistry
  }

  override def clearAllExpectations(): Unit = methodRegistry.clear()

  override def getAllExpectations: Set[(Expectation, Response)] = for {
    (method, pathRegistry) <- methodRegistry.toSet
    (path, queryParamRegistry) <- pathRegistry
    (queryParams, contentRegistry) <- queryParamRegistry
    (content, headerParamsRegistry) <- contentRegistry
    (headerParams, response) <- headerParamsRegistry
  } yield (Expectation(method, path, queryParams, headerParams, content), response)
}
