package com.dzegel.DynamockServer.service

import java.io._

import com.dzegel.DynamockServer.types.{Expectation, Response}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.json4s.jackson.Serialization.{read, write}
import com.google.inject.ImplementedBy
import org.json4s.DefaultFormats

import scala.io.Source

@ImplementedBy(classOf[DefaultExpectationsFileService])
trait ExpectationsFileService {
  def storeExpectationsAsJson(fileName: String, obj: Set[(Expectation, Response)]): Unit

  def loadExpectationsFromJson(fileName: String): Set[(Expectation, Response)]
}

class DefaultExpectationsFileService extends ExpectationsFileService {
  implicit private val format: DefaultFormats.type = DefaultFormats
  private val root = s"${File.listRoots.head.getCanonicalPath}${File.separator}Dynamock${File.separator}Expectations"
  new File(root).mkdirs()
  private val fileExtension = ".expectations.json"

  override def storeExpectationsAsJson(fileName: String, obj: Set[(Expectation, Response)]): Unit = {
    val bufferedWriter = new FileWriter(new File(root, fileName + fileExtension))
    bufferedWriter.write(write(obj))
    bufferedWriter.close()
  }

  override def loadExpectationsFromJson(fileName: String): Set[(Expectation, Response)] = {
    val source = Source.fromFile(new File(root, fileName + fileExtension))
    val json = source.getLines().mkString("\n")
    source.close()
    read[Set[(Expectation, Response)]](json)
  }
}
