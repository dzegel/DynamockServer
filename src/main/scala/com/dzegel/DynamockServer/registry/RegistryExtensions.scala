package com.dzegel.DynamockServer.registry

import com.dzegel.DynamockServer.types.{Content, Response}

import scala.collection.concurrent.TrieMap
import scala.collection.mutable

private[registry] object RegistryExtensions {

  implicit class MethodRegistryExtensions(methodRegistry: MethodRegistry) {
    def getPathRegistry(method: Method): PathRegistry =
      getFromRegistry(method, methodRegistry, mutable.Map.empty[Path, ContentRegistry])
  }

  implicit class PathRegistryExtensions(pathRegistry: PathRegistry) {
    def getContentRegistry(path: Path): ContentRegistry =
      getFromRegistry(path, pathRegistry, TrieMap.empty[Content, Response])
  }

  private def getFromRegistry[TKey, TValue <: mutable.Map[_, _]]
  (key: TKey, registry: mutable.Map[TKey, TValue], defaultValue: TValue): TValue = {
    if (!registry.contains(key)) synchronized {
      registry.put(key, defaultValue)
    }
    registry(key)
  }

}
