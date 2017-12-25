package com.dzegel.DynamockServer.registry

trait ExpectationsUrlPathBaseRegistry {
  def pathBase: String
}

class DefaultExpectationsUrlPathBaseRegistry(rawPathBase: String) extends ExpectationsUrlPathBaseRegistry {
  override val pathBase: String = rawPathBase match {
    case "" => ""
    case bp if bp.startsWith("/") => bp
    case base => s"/$base"
  }
}
