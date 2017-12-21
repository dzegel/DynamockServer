package com.dzegel.DynamockServer

import scala.collection.concurrent.TrieMap

package object types {
  type Path = String
  type Method = String
  type QueryParams = Map[String, String]
  type HeaderSet = Set[(String, String)]

  type MethodRegistry = TrieMap[Method, PathRegistry]
  type PathRegistry = TrieMap[Path, QueryParamRegistry]
  type QueryParamRegistry = TrieMap[QueryParams, ContentRegistry]
  type ContentRegistry = TrieMap[Content, HeaderParamRegistry]
  type HeaderParamRegistry = TrieMap[HeaderParameters, Response]
}
