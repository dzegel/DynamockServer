package com.dzegel.DynamockServer.types

import org.scalatest.{FunSuite, Matchers}

class ContentTests extends FunSuite with Matchers {

  test("""Content("") equals Content("")""") {
    val content1 = Content("")
    val content2 = Content("")

    content1 should equal(content2)
    content1.hashCode should equal(content2.hashCode)
    content1.isJson shouldBe false
  }

  test("""Content("anything") not equals null""") {
    val content1 = Content("anything")
    val content2 = null

    content1 should not equal content2
    content1.isJson shouldBe false
  }

  test("""Content("1") equals Content("1")""") {
    val content1 = Content("1")
    val content2 = Content("1")

    content1 should equal(content2)
    content1.hashCode should equal(content2.hashCode)
    content1.isJson shouldBe false
  }

  test("""Content("1") not equals Content("")""") {
    val content1 = Content("1")
    val content2 = Content("")

    content1 should not equal content2
    content1.hashCode should not equal content2.hashCode
    content1.isJson shouldBe false
    content2.isJson shouldBe false
  }

  test("""Content("1") not equals Content("2")""") {
    val content1 = Content("1")
    val content2 = Content("2")

    content1 should not equal content2
    content1.hashCode should not equal content2.hashCode
    content1.isJson shouldBe false
    content2.isJson shouldBe false
  }

  test("""Content("{}") not equals Content("2")""") {
    val content1 = Content("{}")
    val content2 = Content("2")

    content1 should not equal content2
    content1.hashCode should not equal content2.hashCode
    content1.isJson shouldBe true
    content2.isJson shouldBe false
  }

  test("""Content("   {  }       ") equals Content("{}")""") {
    val content1 = Content("{}")
    val content2 = Content("{}")

    content1 should equal(content2)
    content1.hashCode should equal(content2.hashCode)
    content1.isJson shouldBe true
  }

  test("""Content("{"thing":1}") not equals Content("{"thing":2}")""") {
    val content1 = Content("""{"thing":1}""")
    val content2 = Content("""{"thing":2}""")

    content1 should not equal content2
    content1.hashCode should not equal content2.hashCode
    content1.isJson shouldBe true
    content2.isJson shouldBe true
  }

  test("""Content(" { "thing" : 2 } ") equals Content("{"thing":2}")""") {
    val content1 = Content(""" { "thing" : 2 } """)
    val content2 = Content("""{"thing":2}""")

    content1 should equal(content2)
    content1.hashCode should equal(content2.hashCode)
    content1.isJson shouldBe true
  }

  test("""Content(" [{ "thing" : 2 }  ,    {"otherThing":      "Some String"}     ]") equals Content("[{"otherThing":"Some String"},{"thing":2}]")""") {
    val content1 = Content(""" [{ "thing" : 2 }  ,    {"otherThing":      "Some String"}     ]""")
    val content2 = Content("""[{"otherThing":"Some String"},{"thing":2}]""")

    content1 should equal(content2)
    content1.hashCode should equal(content2.hashCode)
    content1.isJson shouldBe true
    content2.isJson shouldBe true
  }

  test("""Content(" [{ "thing" : 2 }  ,    {"otherThing":      "Some String"}     ]") equals Content("[{"thing":2},{"otherThing":"Some String"}]")""") {
    val content1 = Content(""" [{ "thing" : 2 }  ,    {"otherThing":      "Some String"}     ]""")
    val content2 = Content("""[{"thing":2},{"otherThing":"Some String"}]""")

    content1 should equal(content2)
    content1.hashCode should equal(content2.hashCode)
    content1.isJson shouldBe true
  }

  test("""Content(" [{ "thing" : 2 }  ,    {"otherThing":      "Some String"}     ]") not equals Content("[{thing:2},{"otherThing":"Some String"}]")""") {
    val content1 = Content(""" [{ "thing" : 2 }  ,    {"otherThing":      "Some String"}     ]""")
    val content2 = Content("""[{thing:2},{"otherThing":"Some String"}]""")

    content1 should not equal content2
    content1.hashCode should not equal content2.hashCode
    content1.isJson shouldBe true
    content2.isJson shouldBe false
  }
}
