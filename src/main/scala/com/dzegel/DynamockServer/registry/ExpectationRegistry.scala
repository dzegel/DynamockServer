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
    getContentRegistry(expectation).put(expectation.content, response)

  override def getResponse(expectation: Expectation): Option[Response] =
    getContentRegistry(expectation).get(expectation.content)

  private def getContentRegistry(expectation: Expectation): ContentRegistry = {
    val pathRegistry = getFromRegistry(methodRegistry, expectation.method)
    val queryParamRegistry = getFromRegistry(pathRegistry, expectation.path)
    val contentRegistry = getFromRegistry(queryParamRegistry, expectation.queryParams)
    contentRegistry
  }

  private def getFromRegistry[TKey, TValue](registry: TrieMap[TKey, TValue], key: TKey)
    (implicit m: Manifest[TValue]): TValue = {
    if (!registry.contains(key)) {
      registry.put(key, m.runtimeClass.newInstance.asInstanceOf[TValue])
    }
    registry(key)
  }
}
