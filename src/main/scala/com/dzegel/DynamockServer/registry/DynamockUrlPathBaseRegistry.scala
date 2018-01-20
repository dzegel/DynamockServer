package com.dzegel.DynamockServer.registry

trait DynamockUrlPathBaseRegistry {
  def pathBase: String
}

class DefaultDynamockUrlPathBaseRegistry(rawPathBase: String) extends DynamockUrlPathBaseRegistry {
  override val pathBase: String = rawPathBase match {
    case "" => ""
    case base if base.startsWith("/") => base
    case base => s"/$base"
  }
}
