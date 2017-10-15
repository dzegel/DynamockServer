package com.dzegel.DynamockServer.types

class Content(val stringValue: String) {

  //TODO add json support

  override def hashCode(): Int = stringValue.hashCode

  override def equals(obj: scala.Any): Boolean = {
    if (obj == null || !obj.isInstanceOf[Content]) {
      false
    } else {
      stringValue.equals(obj.asInstanceOf[Content].stringValue)
    }
  }

  override def toString: String = s"Content($stringValue)"
}

object Content {
  def apply(stringValue: String): Content = new Content(stringValue)
}
