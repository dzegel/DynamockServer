package com.dzegel.DynamockServer.types

import org.scalatest.{FunSuite, Matchers}

class ContentTests extends FunSuite with Matchers {

  test("""Content("") equals Content("")""") {
    val content1 = Content("")
    val content2 = Content("")

    content1 should equal(content2)
    content1.hashCode should equal(content2.hashCode)
  }

  test("""Content("anything") not equals null""") {
    val content1 = Content("anything")
    val content2 = null

    content1 should not equal content2
  }

  test("""Content("1") equals Content("1")""") {
    val content1 = Content("1")
    val content2 = Content("1")

    content1 should equal(content2)
    content1.hashCode should equal(content2.hashCode)
  }

  test("""Content("1") not equals Content("")""") {
    val content1 = Content("1")
    val content2 = Content("")

    content1 should not equal content2
    content1.hashCode should not equal content2.hashCode
  }

  test("""Content("1") not equals Content("2")""") {
    val content1 = Content("1")
    val content2 = Content("2")

    content1 should not equal content2
    content1.hashCode should not equal content2.hashCode
  }

  test("""Content("{}") not equals Content("2")""") {
    val content1 = Content("{}")
    val content2 = Content("2")

    content1 should not equal content2
    content1.hashCode should not equal content2.hashCode
  }

  test("""Content("{}") equals Content("{}")""") {
    val content1 = Content("{}")
    val content2 = Content("{}")

    content1 should equal(content2)
    content1.hashCode should equal(content2.hashCode)
  }

  test("""Content("{"thing":1}") not equals Content("{"thing":2}")""") {
    val content1 = Content("""{"thing":1}""")
    val content2 = Content("""{"thing":2}""")

    content1 should not equal content2
    content1.hashCode should not equal content2.hashCode
  }

  test("""Content("{"thing":2}") not equals Content("{"thing":2}")""") {
    val content1 = Content("""{"thing":2}""")
    val content2 = Content("""{"thing":2}""")

    content1 should equal(content2)
    content1.hashCode should equal(content2.hashCode)
  }
}
