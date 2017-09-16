package com.dzegel.DynamockServer

import com.dzegel.DynamockServer.contract.Response

import scala.collection.mutable
import scala.collection.concurrent

package object registry {
  type Path = String
  type Method = String
  type StringContent = String
  type PathRegistry = mutable.Map[Path, MethodRegistry]
  type MethodRegistry = mutable.Map[Method, ContentRegistry]
  type ContentRegistry = concurrent.Map[StringContent, Response]
}
