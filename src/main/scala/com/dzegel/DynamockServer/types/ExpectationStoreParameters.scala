package com.dzegel.DynamockServer.types

trait ExpectationStoreParameters {
  val method: Method
  val path: Path
  val queryParams: QueryParams
  val content: Content
}
