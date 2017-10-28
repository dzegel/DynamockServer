package com.dzegel.DynamockServer.types

import com.dzegel.DynamockServer.registry.{HeaderSet, Method, Path, QueryParams}

case class Request(
  method: Method,
  path: Path,
  queryParams: QueryParams,
  headers: HeaderSet,
  content: Content
) extends RegistryParameters
