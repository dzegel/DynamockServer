package com.dzegel.DynamockServer.service

import org.scalatest.{BeforeAndAfterEach, FunSuite, Matchers}

class HitCountServiceTests extends FunSuite with BeforeAndAfterEach with Matchers {
  private var hitCountService: HitCountService = _

  private val expectationId1 = "id_1"
  private val expectationId2 = "id_2"

  override protected def beforeEach(): Unit = {
    hitCountService = new DefaultHitCountService()
  }

  test("get for unregistered ids returns empty") {
    hitCountService.get(Seq(expectationId1, expectationId2)) shouldBe empty
  }

  test("register initializes to 0") {
    hitCountService.register(Seq(expectationId1, expectationId2))

    hitCountService.get(Seq(expectationId1, expectationId2)) shouldBe Map(expectationId1 -> 0, expectationId2 -> 0)
  }

  test("get only returns for registered ids") {
    hitCountService.register(Seq(expectationId1))

    hitCountService.get(Seq(expectationId1, expectationId2)) shouldBe Map(expectationId1 -> 0)
  }

  test("increment increments") {
    hitCountService.register(Seq(expectationId1, expectationId2))

    hitCountService.increment(Seq(expectationId1, expectationId2))
    hitCountService.get(Seq(expectationId1, expectationId2)) shouldBe Map(expectationId1 -> 1, expectationId2 -> 1)

    hitCountService.increment(Seq(expectationId2))
    hitCountService.get(Seq(expectationId1, expectationId2)) shouldBe Map(expectationId1 -> 1, expectationId2 -> 2)
  }

  test("increment throws for unregistered ids") {
    assertThrows[NoSuchElementException](hitCountService.increment(Seq(expectationId1)))

    hitCountService.register(Seq(expectationId1))

    hitCountService.increment(Seq(expectationId1)) //Does not throw

    assertThrows[NoSuchElementException](hitCountService.increment(Seq(expectationId1, expectationId2)))
  }

  test("register does not reset count for preregistered id") {
    hitCountService.register(Seq(expectationId1, expectationId2))
    hitCountService.increment(Seq(expectationId1))

    hitCountService.register(Seq(expectationId1))

    hitCountService.get(Seq(expectationId1, expectationId2)) shouldBe Map(expectationId1 -> 1, expectationId2 -> 0)
  }

  test("delete only deletes the ids specified") {
    hitCountService.get(Seq(expectationId1, expectationId2)) shouldBe empty

    hitCountService.register(Seq(expectationId1, expectationId2))
    hitCountService.get(Seq(expectationId1, expectationId2)) shouldBe Map(expectationId1 -> 0, expectationId2 -> 0)

    hitCountService.delete(Seq(expectationId1))
    hitCountService.get(Seq(expectationId1, expectationId2)) shouldBe Map(expectationId2 -> 0)

    hitCountService.delete(Seq(expectationId2))
    hitCountService.get(Seq(expectationId1, expectationId2)) shouldBe empty
  }

  test("deleteAll deletes all") {
    hitCountService.get(Seq(expectationId1, expectationId2)) shouldBe empty

    hitCountService.register(Seq(expectationId1, expectationId2))
    hitCountService.get(Seq(expectationId1, expectationId2)) shouldBe Map(expectationId1 -> 0, expectationId2 -> 0)

    hitCountService.deleteAll()
    hitCountService.get(Seq(expectationId1, expectationId2)) shouldBe empty
  }
}
