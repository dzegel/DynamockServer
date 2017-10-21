package com.dzegel.DynamockServer

import com.dzegel.DynamockServer.types.{Content, Response}

import scala.collection.concurrent.TrieMap

package object registry {
  type Path = String
  type Method = String
  type QueryParams = Map[String, String]
  type MethodRegistry = TrieMap[Method, PathRegistry]
  type PathRegistry = TrieMap[Path, QueryParamRegistry]
  type QueryParamRegistry = TrieMap[QueryParams, ContentRegistry]
  type ContentRegistry = TrieMap[Content, Response]
}
