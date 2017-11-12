package com.dzegel.DynamockServer.util

import org.json4s.JsonAST._
import org.scalatest.{FunSuite, Matchers}

import scala.util.Try

class JsonUtilTests extends FunSuite with Matchers {

  private val jsonObjectWithNestedArrays = JObject(List(
    JField("array1", JArray(List(JInt(1), JInt(2), JInt(3), JInt(4), JInt(5), JInt(3)))),
    JField("array2", JArray(List(JObject(), JInt(3), JString("a string"), JDecimal(7.9), JArray(
      List(JString("a"), JString("b"), JString("c"), JString("c"), JString("d"))
    )))),
    JField("int1", JInt(5)),
    JField("double1", JDouble(8.2)),
    JField("string1", JString("another string")),
    JField("object1", JObject(List(
      JField("array2", JArray(List(JDouble(1.1), JDouble(2.2), JDouble(3.3), JDouble(2.2)))),
      JField("object2", JObject(List(
        JField("SomeOtherThing", JObject())
      )))
    )))
  ))

  private val jsonObjectWithNestedSets = JObject(List(
    JField("array1", JSet(Set(JInt(1), JInt(2), JInt(3), JInt(4), JInt(5)))),
    JField("array2", JSet(Set(JObject(), JInt(3), JString("a string"), JDecimal(7.9), JSet(
      Set(JString("a"), JString("b"), JString("c"), JString("d"))
    )))),
    JField("int1", JInt(5)),
    JField("double1", JDouble(8.2)),
    JField("string1", JString("another string")),
    JField("object1", JObject(List(
      JField("array2", JSet(Set(JDouble(1.1), JDouble(2.2), JDouble(3.3)))),
      JField("object2", JObject(List(
        JField("SomeOtherThing", JObject())
      )))
    )))
  ))

  test("Json object with nested arrays should become json object with nested sets") {
    JsonUtil.convertJArraysToJSets(jsonObjectWithNestedArrays) should equal(jsonObjectWithNestedSets)
  }

  test("Json Array with nested arrays should become json object with nested sets") {
    val jsonArrayWithNestedArrays = JArray(List(jsonObjectWithNestedArrays, jsonObjectWithNestedArrays))
    val jsonSetWithNestedSets = JSet(Set(jsonObjectWithNestedSets))

    JsonUtil.convertJArraysToJSets(jsonArrayWithNestedArrays) should equal(jsonSetWithNestedSets)
  }

  test("Json values other than object and array should throw") {
    Try(JsonUtil.convertJArraysToJSets(JBool(false))).isFailure shouldBe true
    Try(JsonUtil.convertJArraysToJSets(JLong(1))).isFailure shouldBe true
    Try(JsonUtil.convertJArraysToJSets(JInt(1))).isFailure shouldBe true
    Try(JsonUtil.convertJArraysToJSets(JDouble(1.1))).isFailure shouldBe true
    Try(JsonUtil.convertJArraysToJSets(JDecimal(1.5))).isFailure shouldBe true
    Try(JsonUtil.convertJArraysToJSets(JString("sdf"))).isFailure shouldBe true
    Try(JsonUtil.convertJArraysToJSets(JSet(Set(JString("gf"))))).isFailure shouldBe true
    Try(JsonUtil.convertJArraysToJSets(JNothing)).isFailure shouldBe true
    Try(JsonUtil.convertJArraysToJSets(JNull)).isFailure shouldBe true
  }
}
