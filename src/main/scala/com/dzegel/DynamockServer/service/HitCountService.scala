package com.dzegel.DynamockServer.service

import com.dzegel.DynamockServer.types.ExpectationId
import com.google.inject.{ImplementedBy, Singleton}

import scala.collection.mutable

@ImplementedBy(classOf[DefaultHitCountService])
trait HitCountService {
  def register(expectationIds: Seq[ExpectationId]): Unit

  def get(expectationIds: Seq[ExpectationId]): Map[ExpectationId, Int]

  def increment(expectationIds: Seq[ExpectationId]): Unit

  def delete(expectationIds: Seq[ExpectationId]): Unit

  def deleteAll(): Unit
}

@Singleton
class DefaultHitCountService extends HitCountService {
  val hitCounts = mutable.Map.empty[ExpectationId, Int]

  override def register(expectationIds: Seq[ExpectationId]): Unit = expectationIds
    .filterNot(expectationId => hitCounts.contains(expectationId))
    .foreach(expectationId => hitCounts(expectationId) = 0)

  override def get(expectationIds: Seq[ExpectationId]): Map[ExpectationId, Int] = expectationIds
    .filter(hitCounts.contains)
    .map(expectationId => expectationId -> hitCounts(expectationId))
    .toMap

  override def increment(expectationIds: Seq[ExpectationId]): Unit = expectationIds
    .foreach(expectationId => hitCounts(expectationId) = hitCounts(expectationId) + 1)

  override def delete(expectationIds: Seq[ExpectationId]): Unit = expectationIds
    .foreach(expectationId => hitCounts.remove(expectationId))

  override def deleteAll(): Unit = hitCounts.clear()
}
