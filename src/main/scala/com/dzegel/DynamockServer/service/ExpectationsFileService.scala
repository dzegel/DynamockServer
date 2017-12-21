package com.dzegel.DynamockServer.service

import java.io._

import com.dzegel.DynamockServer.types.{Expectation, Response}
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.google.inject.ImplementedBy

@ImplementedBy(classOf[DefaultExpectationsFileService])
trait ExpectationsFileService {
  def storeExpectationsAsJson(fileName: String, obj: Set[(Expectation, Response)]): Unit

  def loadExpectationsFromJson(fileName: String): Set[(Expectation, Response)]
}

class DefaultExpectationsFileService extends ExpectationsFileService {
  private val objectMapper = new ObjectMapper with ScalaObjectMapper {
    registerModule(DefaultScalaModule)
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  }
  private val root = s"${File.listRoots.head.getCanonicalPath}${File.separator}Dynamock${File.separator}Expectations"
  new File(root).mkdirs()

  override def storeExpectationsAsJson(fileName: String, obj: Set[(Expectation, Response)]): Unit =
    objectMapper.writeValue(makeFile(fileName), obj)

  override def loadExpectationsFromJson(fileName: String): Set[(Expectation, Response)] =
    objectMapper.readValue[Set[(Expectation, Response)]](makeFile(fileName))

  private def makeFile(fileName: String): File = new File(root, fileName + ".expectations.json")
}
