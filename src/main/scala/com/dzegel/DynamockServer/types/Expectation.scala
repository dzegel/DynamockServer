package com.dzegel.DynamockServer.types

import com.dzegel.DynamockServer.registry.{HeaderParams, Method, Path, QueryParams}

case class Expectation(
  method: Method,
  path: Path,
  queryParams: QueryParams,
  includedHeaderParameters: HeaderParams,
  content: Content)
