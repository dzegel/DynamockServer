package com.dzegel.DynamockServer.service

import java.io.File

import com.dzegel.DynamockServer.types.{Content, Expectation, HeaderParameters, Response}
import org.scalatest.{BeforeAndAfterEach, FunSuite, Matchers}

import scala.util.Random

class ExpectationsFileServiceTests extends FunSuite with Matchers with BeforeAndAfterEach {
  private val expectationsFileService = new DefaultExpectationsFileService

  private val expectation1 = Expectation("GET", "/", Map(), HeaderParameters(Set(), Set()), Content("Some Stuff"))
  private val expectation2 = Expectation("PUT", "/uri", Map("query1" -> "query2"), HeaderParameters(Set("header1" -> "header2"), Set()), Content("{}"))
  private val response1 = Response(200, "stuff", Map())
  private val response2 = Response(300, "other stuff", Map())
  private val expectations = Set(expectation1 -> response1, expectation2 -> response2)
  private val fileName = "testFile_" + Random.nextInt()

  override protected def afterEach: Unit = {
    val fileRoot = s"${File.listRoots.head.getCanonicalPath}${File.separator}Dynamock${File.separator}Expectations"
    new File(fileRoot, fileName + ".expectations.json").delete()
  }

  test("storeExpectationsAsJson and loadExpectationsFromJson work") {
    expectationsFileService.storeExpectationsAsJson(fileName, expectations)
    val result = expectationsFileService.loadExpectationsFromJson(fileName)

    result should equal(expectations)
  }
}
