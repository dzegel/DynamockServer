package com.dzegel.DynamockServer.service

import java.io._

import com.dzegel.DynamockServer.registry.FileRootRegistry
import com.dzegel.DynamockServer.service.DefaultExpectationsFileService._
import com.dzegel.DynamockServer.types.{Expectation, Response}
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.google.inject.{ImplementedBy, Inject, Singleton}

@ImplementedBy(classOf[DefaultExpectationsFileService])
trait ExpectationsFileService {
  def storeExpectationsAsJson(fileName: String, obj: Set[(Expectation, Option[Response])]): Unit

  def loadExpectationsFromJson(fileName: String): Set[(Expectation, Option[Response])]
}

@Singleton
class DefaultExpectationsFileService @Inject()(fileRootRegistry: FileRootRegistry) extends ExpectationsFileService {
  private val objectMapper = new ObjectMapper with ScalaObjectMapper {
    registerModule(DefaultScalaModule)
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  }
  private val fileRoot = s"${fileRootRegistry.fileRoot}${File.separator}Expectations"
  new File(fileRoot).mkdirs()

  override def storeExpectationsAsJson(fileName: String, obj: Set[(Expectation, Option[Response])]): Unit =
    objectMapper.writeValue(makeFile(fileName), obj.map { case (expectation, response) => SerializationWrapper(expectation, response) })

  override def loadExpectationsFromJson(fileName: String): Set[(Expectation, Option[Response])] =
    objectMapper.readValue[Set[SerializationWrapper]](makeFile(fileName)).map(wrapper => (wrapper.expectation, wrapper.response))

  private def makeFile(fileName: String): File = new File(fileRoot, fileName + ".expectations.json")
}

private object DefaultExpectationsFileService {

  private case class SerializationWrapper(expectation: Expectation, response: Option[Response])

}
