package com.dzegel.DynamockServer.service

import java.io._

import com.dzegel.DynamockServer.registry.FileRootRegistry
import com.dzegel.DynamockServer.types.{ExpectationId, ExpectationResponse}
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.google.inject.{ImplementedBy, Inject, Singleton}

@ImplementedBy(classOf[DefaultExpectationsFileService])
trait ExpectationsFileService {
  def storeExpectationsAsJson(fileName: String, obj: Set[(ExpectationId, ExpectationResponse)]): Unit

  def loadExpectationsFromJson(fileName: String): Set[(ExpectationId, ExpectationResponse)]
}

@Singleton
class DefaultExpectationsFileService @Inject()(fileRootRegistry: FileRootRegistry) extends ExpectationsFileService {
  private val objectMapper = new ObjectMapper with ScalaObjectMapper {
    registerModule(DefaultScalaModule)
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  }
  private val fileRoot = s"${fileRootRegistry.fileRoot}${File.separator}Expectations"
  new File(fileRoot).mkdirs()

  override def storeExpectationsAsJson(fileName: String, obj: Set[(ExpectationId, ExpectationResponse)]): Unit =
    objectMapper.writeValue(makeFile(fileName), obj)

  override def loadExpectationsFromJson(fileName: String): Set[(ExpectationId, ExpectationResponse)] =
    objectMapper.readValue[Set[(ExpectationId, ExpectationResponse)]](makeFile(fileName))

  private def makeFile(fileName: String): File = new File(fileRoot, fileName + ".expectations.json")
}
