package com.dzegel.DynamockServer.service

import com.google.inject.ImplementedBy

@ImplementedBy(classOf[DefaultFileService])
trait FileService {
  def storeObjectAsJson(fileName: String, obj: Object): Unit

  def loadObjectFromJson[T](fileName: String): T
}

class DefaultFileService extends FileService {
  override def storeObjectAsJson(fileName: String, obj: Object): Unit = ???

  override def loadObjectFromJson[T](fileName: String): T = ???
}
