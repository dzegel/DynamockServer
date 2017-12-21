package com.dzegel.DynamockServer.service

// These are injected in DynamockServer.runTimeInjectionModule

trait PortNumberRegistry {
  val portNumber: String
}

trait FileRootRegistry {
  val fileRoot: String
}

class ValueInjectionRegistry(override val portNumber: String, override val fileRoot: String)
  extends PortNumberRegistry with FileRootRegistry
