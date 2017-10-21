package com.dzegel.DynamockServer.types

import com.dzegel.DynamockServer.registry.{Method, Path, QueryParams}

case class Expectation(
  method: Method,
  path: Path,
  queryParams: QueryParams,
  includedHeaderParameters: Map[String, String],
  content: Content)
