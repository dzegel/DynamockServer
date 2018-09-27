package com.dzegel.DynamockServer.service

import com.dzegel.DynamockServer.types.{DidOverwriteResponse, ExpectationId, Response}
import com.google.inject.{ImplementedBy, Singleton}

import scala.collection.concurrent.TrieMap

@ImplementedBy(classOf[DefaultResponseStore])
trait ResponseStore {
  def registerResponses(expectationIdToResponse: Map[ExpectationId, Response]): Map[ExpectationId, DidOverwriteResponse]

  def getResponses(expectationIds: Set[ExpectationId]): Map[ExpectationId, Response]

  def deleteResponses(expectationIds: Set[ExpectationId]): Unit
}

@Singleton
class DefaultResponseStore extends ResponseStore {
  private val expectationIdToResponse = TrieMap.empty[ExpectationId, Response]

  override def registerResponses(expectationIdToResponse: Map[ExpectationId, Response])
  : Map[ExpectationId, DidOverwriteResponse] = this.synchronized {
    expectationIdToResponse.map { case (expectationId, response) =>
      val overwritingResponse = this.expectationIdToResponse.get(expectationId).exists(_ != response)
      this.expectationIdToResponse.put(expectationId, response)
      (expectationId, overwritingResponse)
    }
  }

  override def getResponses(expectationIds: Set[ExpectationId]): Map[ExpectationId, Response] = this.synchronized {
    expectationIds
      .map(id => (id, expectationIdToResponse.get(id)))
      .collect { case (id, Some(response)) => (id, response) }
      .toMap
  }

  override def deleteResponses(expectationIds: Set[ExpectationId]): Unit = this.synchronized {
    expectationIds.foreach(expectationIdToResponse.remove)
  }
}
