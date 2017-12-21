package com.dzegel.DynamockServer.types

trait RegistryParameters {
  val method: Method
  val path: Path
  val queryParams: QueryParams
  val content: Content
}
