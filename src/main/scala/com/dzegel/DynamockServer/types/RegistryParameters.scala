package com.dzegel.DynamockServer.types

import com.dzegel.DynamockServer.registry.{Method, Path, QueryParams}

trait RegistryParameters {
  val method: Method
  val path: Path
  val queryParams: QueryParams
  val content: Content
}
