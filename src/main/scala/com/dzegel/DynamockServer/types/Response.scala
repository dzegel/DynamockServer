package com.dzegel.DynamockServer.types

case class Response(status: Int, content: String = "", headerMap: Map[String, String] = Map.empty)
