package com.dzegel.DynamockServer.registry

import com.dzegel.DynamockServer.contract.{Expectation, Response}
import com.dzegel.DynamockServer.registry.RegistryExtensions._
import com.google.inject.ImplementedBy

import scala.collection.mutable

@ImplementedBy(classOf[DefaultExpectationRegistry])
trait ExpectationRegistry {
  def registerExpectationWithResponse(expectation: Expectation, response: Response): Unit

  def getResponse(expectation: Expectation): Option[Response]
}

class DefaultExpectationRegistry extends ExpectationRegistry {

  private val pathRegistry = mutable.Map.empty[Path, MethodRegistry]

  override def registerExpectationWithResponse(expectation: Expectation, response: Response): Unit =
    getContentRegistry(expectation).put(expectation.stringContent, response)

  override def getResponse(expectation: Expectation): Option[Response] =
    getContentRegistry(expectation).get(expectation.stringContent)

  private def getContentRegistry(expectation: Expectation): ContentRegistry = {
    val methodRegistry = pathRegistry.getMethodRegistry(expectation.path)
    methodRegistry.getContentRegistry(expectation.method)
  }
}
