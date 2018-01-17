package com.dzegel.DynamockServer.registry

trait ExpectationsUrlPathBaseRegistry {
  def pathBase: String
}

class DefaultExpectationsUrlPathBaseRegistry(rawPathBase: String) extends ExpectationsUrlPathBaseRegistry {
  override val pathBase: String = rawPathBase match {
    case "" => ""
    case base if base.startsWith("/") => base
    case base => s"/$base"
  }
}
