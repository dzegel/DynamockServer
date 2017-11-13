package com.dzegel.DynamockServer.util

import org.json4s.JsonAST.{JField, JSet}
import org.json4s.{JArray, JObject, JValue}

object JsonUtil {
  def convertJArraysToJSets(jValue: JValue): JValue = jValue match {
    case jValue: JObject => convertJArraysToJSetsHelper(jValue)
    case jValue: JArray => convertJArraysToJSetsHelper(jValue)
    case _ => throw new Exception(s"JArray to JSet conversion failed, $jValue is not a JObject or JArray.")
  }

  private def convertJArraysToJSetsHelper(jValue: JValue): JValue = jValue match {
    case JObject(jFields) => JObject(jFields.map {
      case JField(name, value) => JField(name, convertJArraysToJSetsHelper(value))
    })
    case JArray(jValues) => JSet(jValues.map(convertJArraysToJSetsHelper).toSet)
    case _ => jValue
  }
}
