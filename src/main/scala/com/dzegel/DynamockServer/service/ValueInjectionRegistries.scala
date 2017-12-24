package com.dzegel.DynamockServer.service

// These are injected in DynamockServer.runTimeInjectionModule

class PortNumberRegistry(val portNumber: String)

class FileRootRegistry(val fileRoot: String)

class ExpectationsUrlPathBaseRegistry(val pathBase: String)
