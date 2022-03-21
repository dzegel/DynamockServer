package com.dzegel.DynamockServer.service

import java.io.File

import com.dzegel.DynamockServer.registry.FileRootRegistry
import com.dzegel.DynamockServer.types.{Content, Expectation, HeaderParameters, Response}
import org.scalatest.{BeforeAndAfterEach, FunSuite, Matchers}

import scala.util.Random

class ExpectationsFileServiceTests extends FunSuite with Matchers with BeforeAndAfterEach {
  private val fileRootRegistry = new FileRootRegistry {
    override val fileRoot = s"${File.listRoots.head.getCanonicalPath}${File.separator}DynamockServer${File.separator}ExpectationsFileServiceTests"
  }
  private val expectationsFileService = new DefaultExpectationsFileService(fileRootRegistry)

  private val expectation1 = Expectation("GET", "/", Set(), HeaderParameters(Set(), Set()), Content("Some Stuff"))
  private val expectation2 = Expectation("PUT", "/uri", Set("query1" -> "query2"), HeaderParameters(Set("header1" -> "header2"), Set()), Content("{}"))
  private val expectation3 = Expectation("POST", "/stuff", Set("query3" -> "query4"), HeaderParameters(Set(), Set()), Content(""))
  private val response1 = Response(200, "stuff", Map())
  private val response2 = Response(300, "other stuff", Map())
  private val idAndExpectationResponses = Set((expectation1, Some(response1)), (expectation2, Some(response2)), (expectation3, None))
  private val fileName = "testFile_" + Random.nextInt()

  override protected def afterEach: Unit = getFile.delete()

  test("storeExpectationsAsJson and loadExpectationsFromJson work") {
    getFile.exists() shouldBe false

    expectationsFileService.storeExpectationsAsJson(fileName, idAndExpectationResponses)
    val result = expectationsFileService.loadExpectationsFromJson(fileName)

    result should equal(idAndExpectationResponses)
    getFile.exists() shouldBe true
  }

  private def getFile: File = new File(s"${fileRootRegistry.fileRoot}${File.separator}Expectations", s"$fileName.expectations.json")
}
