package com.dzegel.DynamockServer.types

case class Expectation(
  method: Method,
  path: Path,
  queryParams: QueryParams,
  headerParameters: HeaderParameters,
  content: Content
) extends ExpectationStoreParameters
