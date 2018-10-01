package com.dzegel.DynamockServer

import scala.collection.concurrent.TrieMap

package object types {
  type Path = String
  type Method = String
  type QueryParams = Map[String, String]
  type HeaderSet = Set[(String, String)]
  type ExpectationId = String
  type HeaderParamRegistry = TrieMap[HeaderParameters, ExpectationId]
  type ExpectationResponse = (Expectation, Response)
  type DidOverwriteResponse = Boolean
}
