package com.dzegel.DynamockServer.Registry

import scala.collection.concurrent.TrieMap
import scala.collection.mutable
object RegistryExtensions {

  implicit class PathRegistryExtensions(pathRegistry: PathRegistry) {
    def getMethodRegistry(path: Path): MethodRegistry =
      getFromRegistry(path, pathRegistry, mutable.Map.empty[Method, ContentRegistry])
  }

  implicit class MethodRegistryExtensions(methodRegistry: MethodRegistry) {
    def getContentRegistry(method: Method): ContentRegistry =
      getFromRegistry(method, methodRegistry, TrieMap.empty[StringContent, Response])
  }

  private def getFromRegistry[TKey, TValue <: mutable.Map[_, _]]
  (key: TKey, registry: mutable.Map[TKey, TValue], defaultValue: TValue): TValue = {
    if (!registry.contains(key)) synchronized {
      registry.put(key, defaultValue)
    }
    registry(key)
  }

}
