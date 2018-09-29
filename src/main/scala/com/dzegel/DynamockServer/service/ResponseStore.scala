package com.dzegel.DynamockServer.service

import com.dzegel.DynamockServer.types.{DidOverwriteResponse, ExpectationId, Response}
import com.google.inject.{ImplementedBy, Singleton}

import scala.collection.concurrent.TrieMap

@ImplementedBy(classOf[DefaultResponseStore])
trait ResponseStore {
  def registerResponse(expectationId: ExpectationId, response: Response): DidOverwriteResponse

  def getResponses(expectationIds: Set[ExpectationId]): Map[ExpectationId, Response]

  def deleteResponses(expectationIds: Set[ExpectationId]): Unit

  def clearAllResponses(): Unit
}

@Singleton
class DefaultResponseStore extends ResponseStore {
  private val expectationIdToResponse = TrieMap.empty[ExpectationId, Response]

  override def registerResponse(expectationId: ExpectationId, response: Response): DidOverwriteResponse = this.synchronized {
    val overwritingResponse = this.expectationIdToResponse.get(expectationId).exists(_ != response)
    this.expectationIdToResponse.put(expectationId, response)
    overwritingResponse
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

  override def clearAllResponses(): Unit = this.synchronized {
    expectationIdToResponse.clear()
  }
}
