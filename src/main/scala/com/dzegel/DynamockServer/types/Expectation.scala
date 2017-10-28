package com.dzegel.DynamockServer.types

import com.dzegel.DynamockServer.registry.{Method, Path, QueryParams}

case class Expectation(
  method: Method,
  path: Path,
  queryParams: QueryParams,
  headerParameters: HeaderParameters,
  content: Content
) extends RegistryParameters
