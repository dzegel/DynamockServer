package com.dzegel.DynamockServer.service

import com.dzegel.DynamockServer.types.{Content, Expectation, HeaderParameters, Response}
import org.scalatest.{FunSuite, Matchers}

class ExpectationsFileServiceTests extends FunSuite with Matchers {
  private val expectationsFileService = new DefaultExpectationsFileService

  private val expectation1 = Expectation("GET", "/", Map(), HeaderParameters(Set(), Set()), Content("Some Stuff"))
  private val expectation2 = Expectation("PUT", "/uri", Map("query1" -> "query2"), HeaderParameters(Set("header1" -> "header2"), Set()), Content("{}"))
  private val response1 = Response(200, "stuff", Map())
  private val response2 = Response(300, "other stuff", Map())
  private val expectations = Set(expectation1 -> response1, expectation2 -> response2)
  private val fileName = "testFile"
  test("storeExpectationsAsJson and loadExpectationsFromJson work") {
    expectationsFileService.storeExpectationsAsJson(fileName, expectations)
    val result = expectationsFileService.loadExpectationsFromJson(fileName)

    result should equal(expectations)
  }
}
