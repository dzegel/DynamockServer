package com.dzegel.DynamockServer.registry

import java.io.File

trait FileRootRegistry {
  def fileRoot: String
}

class DefaultFileRootRegistry(port: String) extends FileRootRegistry {
  override val fileRoot: String = s"${File.listRoots.head.getCanonicalPath}${File.separator}DynamockServer${File.separator}$port"
  new File(fileRoot).mkdirs()
}
