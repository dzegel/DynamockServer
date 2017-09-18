package com.dzegel.DynamockServer.contract

case class Response(status: Int, content: String = "", headerMap: Map[String, String] = Map.empty)
