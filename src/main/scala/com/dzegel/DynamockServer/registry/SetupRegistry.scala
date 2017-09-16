package com.dzegel.DynamockServer.registry

import com.dzegel.DynamockServer.contract.{Expectation, Response}
import com.dzegel.DynamockServer.registry.RegistryExtensions._
import com.google.inject.{ImplementedBy, Singleton}

import scala.collection.mutable

@ImplementedBy(classOf[DefaultSetupRegistry])
trait SetupRegistry {
  def registerExpectationWithResponse(expectation: Expectation, response: Response): Unit

  def getResponse(expectation: Expectation): Option[Response]
}

@Singleton
class DefaultSetupRegistry extends SetupRegistry {

  private val pathRegistry = mutable.Map.empty[Path, MethodRegistry]

  override def registerExpectationWithResponse(expectation: Expectation, response: Response): Unit =
    getContentRegistry(expectation).put(expectation.content, response)

  override def getResponse(expectation: Expectation): Option[Response] =
    getContentRegistry(expectation).get(expectation.content)

  private def getContentRegistry(expectation: Expectation): ContentRegistry = {
    val methodRegistry = pathRegistry.getMethodRegistry(expectation.path)
    methodRegistry.getContentRegistry(expectation.method)
  }
}
