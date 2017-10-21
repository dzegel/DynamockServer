package com.dzegel.DynamockServer.registry

import com.dzegel.DynamockServer.types.{Expectation, Response}
import com.google.inject.{ImplementedBy, Singleton}

import scala.collection.concurrent.TrieMap

@ImplementedBy(classOf[DefaultExpectationRegistry])
trait ExpectationRegistry {
  def registerExpectationWithResponse(expectation: Expectation, response: Response): Unit

  def getResponse(expectation: Expectation): Option[Response]
}

@Singleton
class DefaultExpectationRegistry extends ExpectationRegistry {

  private val methodRegistry = TrieMap.empty[Method, PathRegistry]

  override def registerExpectationWithResponse(expectation: Expectation, response: Response): Unit =
    getHeaderParamRegistry(expectation).put(expectation.includedHeaderParameters, response)

  override def getResponse(expectation: Expectation): Option[Response] = {
    getHeaderParamRegistry(expectation)
      .filter { case (headers, response) => headers.toSet.subsetOf(expectation.includedHeaderParameters.toSet) } //find valid options
      .reduceOption[(HeaderParams, Response)] {
        case (left, right) if left._1.size == right._1.size => //if they have the same number of headers pick one deterministically
          if (left._1.hashCode() <= right._1.hashCode()) left else right
        case (left, right) => if (left._1.size > right._1.size) left else right //find the option with the most headers
      }.map { case (headers, response) => response }
  }

  private def getHeaderParamRegistry(expectation: Expectation): HeaderParamRegistry = {
    val pathRegistry = methodRegistry.getOrElseUpdate(expectation.method, TrieMap.empty)
    val queryParamRegistry = pathRegistry.getOrElseUpdate(expectation.path, TrieMap.empty)
    val contentRegistry = queryParamRegistry.getOrElseUpdate(expectation.queryParams, TrieMap.empty)
    val headerParamRegistry = contentRegistry.getOrElseUpdate(expectation.content, TrieMap.empty)
    headerParamRegistry
  }
}
