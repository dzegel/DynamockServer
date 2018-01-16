package com.dzegel.DynamockServer.service

import java.util.UUID

import com.google.inject.{ImplementedBy, Singleton}

@ImplementedBy(classOf[DefaultRandomStringGenerator])
trait RandomStringGenerator {
  def next(): String
}

@Singleton
class DefaultRandomStringGenerator extends RandomStringGenerator {
  override def next(): String = UUID.randomUUID().toString
}
