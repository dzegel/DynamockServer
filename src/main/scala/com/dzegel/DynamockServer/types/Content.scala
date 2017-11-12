package com.dzegel.DynamockServer.types

import org.json4s.native.JsonParser.parseOpt

class Content(val stringValue: String) {

  private val json = parseOpt(stringValue)

  def isJson: Boolean = json.isDefined

  override def hashCode(): Int = json match {
    case Some(jValue) => jValue.hashCode
    case None => stringValue.hashCode
  }

  override def equals(obj: scala.Any): Boolean = obj match {
    case other: Content if other != null => equals(other)
    case _ => false
  }

  def equals(other: Content): Boolean = (json, other.json) match {
    case (Some(thisJValue), Some(otherJValue)) => thisJValue.equals(otherJValue)
    case (None, None) => stringValue.equals(other.stringValue)
    case _ => false
  }

  override def toString: String = s"Content($stringValue)"
}

object Content {
  def apply(stringValue: String): Content = new Content(stringValue)
}
