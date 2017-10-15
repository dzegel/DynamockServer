package com.dzegel.DynamockServer

import com.dzegel.DynamockServer.types.{Content, Response}

import scala.collection.mutable
import scala.collection.concurrent

package object registry {
  type Path = String
  type Method = String
  type MethodRegistry = mutable.Map[Method, PathRegistry]
  type PathRegistry = mutable.Map[Path, ContentRegistry]
  type ContentRegistry = concurrent.Map[Content, Response]
}
