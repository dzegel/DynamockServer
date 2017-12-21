package com.dzegel.DynamockServer.types

case class Request(
  method: Method,
  path: Path,
  queryParams: QueryParams,
  headers: HeaderSet,
  content: Content
) extends RegistryParameters
