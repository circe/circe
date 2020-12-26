package io.circe.pointer

import io.circe.{ CursorOp, Json }
import io.circe.parser
import munit.DisciplineSuite
import org.scalacheck.Prop

class PointerSuite extends DisciplineSuite {
  test("The absolute pointer examples from RFC 6901 should evaluate correctly") {
    val Right(example: Json) = parser.parse(
      """{
        "foo": ["bar", "baz"],
        "": 0,
        "a/b": 1,
        "c%d": 2,
        "e^f": 3,
        "g|h": 4,
        "i\\j": 5,
        "k\"l": 6,
        " ": 7,
        "m~n": 8
      }"""
    )

    val Right(p0) = Pointer.parse("")
    val Right(p1) = Pointer.parse("/foo")
    val Right(p2) = Pointer.parse("/foo/0")
    val Right(p3) = Pointer.parse("/")
    val Right(p4) = Pointer.parse("/a~1b")
    val Right(p5) = Pointer.parse("/c%d")
    val Right(p6) = Pointer.parse("/e^f")
    val Right(p7) = Pointer.parse("/g|h")
    val Right(p8) = Pointer.parse("/i\\j")
    val Right(p9) = Pointer.parse("/k\"l")
    val Right(p10) = Pointer.parse("/ ")
    val Right(p11) = Pointer.parse("/m~0n")

    assertEquals(p0(example.hcursor).focus, Some(example))
    assertEquals(p1(example.hcursor).focus, Some(Json.arr(Json.fromString("bar"), Json.fromString("baz"))))
    assertEquals(p2(example.hcursor).focus, Some(Json.fromString("bar")))
    assertEquals(p3(example.hcursor).focus, Some(Json.fromInt(0)))
    assertEquals(p4(example.hcursor).focus, Some(Json.fromInt(1)))
    assertEquals(p5(example.hcursor).focus, Some(Json.fromInt(2)))
    assertEquals(p6(example.hcursor).focus, Some(Json.fromInt(3)))
    assertEquals(p7(example.hcursor).focus, Some(Json.fromInt(4)))
    assertEquals(p8(example.hcursor).focus, Some(Json.fromInt(5)))
    assertEquals(p9(example.hcursor).focus, Some(Json.fromInt(6)))
    assertEquals(p10(example.hcursor).focus, Some(Json.fromInt(7)))
    assertEquals(p11(example.hcursor).focus, Some(Json.fromInt(8)))

    List(p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11).foreach { p =>
      assert(p.toSubtype.isRight)
    }
  }

  test("The relative pointer examples from the Relative JSON Pointers memo should evaluate correctly") {
    val Right(example: Json) = parser.parse(
      """{
        "foo": ["bar", "baz"],
        "highly": {
          "nested": {
            "objects": true
          }
        }
      }"""
    )

    val original = example.hcursor.downField("foo").downN(1)

    val Right(p0) = Pointer.parse("0")
    val Right(p1) = Pointer.parse("1/0")
    val Right(p2) = Pointer.parse("2/highly/nested/objects")
    val Right(p3) = Pointer.parse("0#")
    val Right(p4) = Pointer.parse("1#")

    val Right(r0) = Pointer.Relative.parse("0")
    val Right(r1) = Pointer.Relative.parse("1/0")
    val Right(r2) = Pointer.Relative.parse("2/highly/nested/objects")
    val Right(r3) = Pointer.Relative.parse("0#")
    val Right(r4) = Pointer.Relative.parse("1#")

    assertEquals(p0(original).focus, Some(Json.fromString("baz")))
    assertEquals(p1(original).focus, Some(Json.fromString("bar")))
    assertEquals(p2(original).focus, Some(Json.fromBoolean(true)))
    assertEquals(p3(original).focus, Some(Json.fromString("baz")))
    assertEquals(p4(original).focus, Some(Json.arr(Json.fromString("bar"), Json.fromString("baz"))))

    assertEquals(r0(original).focus, Some(Json.fromString("baz")))
    assertEquals(r1(original).focus, Some(Json.fromString("bar")))
    assertEquals(r2(original).focus, Some(Json.fromBoolean(true)))
    assertEquals(r3(original).focus, Some(Json.fromString("baz")))
    assertEquals(r4(original).focus, Some(Json.arr(Json.fromString("bar"), Json.fromString("baz"))))

    assertEquals(r0.evaluate(original), Right(Pointer.Relative.Result.Json(Json.fromString("baz"))))
    assertEquals(r1.evaluate(original), Right(Pointer.Relative.Result.Json(Json.fromString("bar"))))
    assertEquals(r2.evaluate(original), Right(Pointer.Relative.Result.Json(Json.fromBoolean(true))))
    assertEquals(r3.evaluate(original), Right(Pointer.Relative.Result.Index(1)))
    assertEquals(r4.evaluate(original), Right(Pointer.Relative.Result.Key("foo")))

    List(p0, p1, p2, p3, p4, r0, r1, r2, r3, r4).foreach { p =>
      assert(p.toSubtype.isLeft)
    }
  }

  test("Tokens with leading zeros should work as object keys") {
    val Right(p) = Pointer.parse("/foo/01/bar")
    val Right(doc) = parser.parse("""{"foo": {"01": {"bar": true}}}""")

    assertEquals(p(doc.hcursor).focus, Some(Json.fromBoolean(true)))
  }

  test("Tokens with leading zeros should not work as array indices") {
    val Right(good) = Pointer.parse("/foo/1/bar")
    val Right(bad) = Pointer.parse("/foo/01/bar")
    val Right(doc) = parser.parse("""{"foo": [{}, {"bar": true}]}""")

    assertEquals(good(doc.hcursor).focus, Some(Json.fromBoolean(true)))
    assertEquals(bad(doc.hcursor).focus, None)
  }

  property("Pointer.parse should never throw exceptions") {
    Prop.forAll { (input: String) =>
      Pointer.parse(input)
      Pointer.parse(input, supportRelative = false)
      true
    }
  }

  test("Pointer parsing should fail with invalid escape sequences") {
    assertEquals(Pointer.parse("/foo/~0/~2/~1"), Left(PointerSyntaxError(9, "0 or 1")))
  }

  test("Pointer parsing should fail with trailing tilde") {
    assertEquals(Pointer.parse("/foo/~0/bar/~"), Left(PointerSyntaxError(12, "token character")))
  }

  test("Pointer parsing should fail on missing root") {
    assertEquals(Pointer.parse("foo/~0/bar"), Left(PointerSyntaxError(0, "/ or digit")))
  }

  test("Pointer parsing should fail on missing root if relative pointers aren't supported") {
    assertEquals(Pointer.parse("foo/~0/bar", supportRelative = false), Left(PointerSyntaxError(0, "/")))
  }

  test("Pointer parsing should fail on relative pointer if relative pointers aren't supported") {
    assertEquals(Pointer.parse("0", supportRelative = false), Left(PointerSyntaxError(0, "/")))
  }

  test("Relative pointer parsing should fail on leading zeros") {
    assertEquals(Pointer.parse("01/foo"), Left(PointerSyntaxError(1, "JSON Pointer or #")))
  }

  test("Relative pointer parsing should fail on extra input") {
    assertEquals(Pointer.parse("1foo"), Left(PointerSyntaxError(1, "JSON Pointer or #")))
  }

  test("Relative pointer parsing should fail on extra input after #") {
    assertEquals(Pointer.parse("0#/foo"), Left(PointerSyntaxError(2, "end of input")))
  }

  test("Relative pointer parsing should fail with an excessively large leading number") {
    assertEquals(Pointer.parse("1234567890/foo"), Left(PointerSyntaxError(9, "JSON Pointer or #")))
  }

  test("Relative pointer parsing should fail without a leading number") {
    assertEquals(Pointer.Relative.parse("/foo"), Left(PointerSyntaxError(0, "digit")))
  }

  test("Pointer navigation should fail as expected") {
    val Right(example) = parser.parse("""{"foo": [1, 2, 3], "bar": null, "baz": false}""")
    val Right(p0) = Pointer.parse("/foo/3")
    val Right(p1) = Pointer.parse("/qux/3")
    val Right(p2) = Pointer.parse("/foo/bar/baz")

    assertEquals(p0.get(example), Left(PointerFailure(List(CursorOp.DownN(3), CursorOp.DownField("foo")))))
    assertEquals(p1.get(example), Left(PointerFailure(List(CursorOp.DownField("qux")))))
    assertEquals(p2.get(example), Left(PointerFailure(List(CursorOp.DownField("bar"), CursorOp.DownField("foo")))))
  }

  test("tokens should return tokens") {
    val Right(p) = Pointer.parse("/foo//bar/~0~1/qux/1/-/baz/")
    val Right(a) = p.toSubtype

    assertEquals(a.tokens, Vector("foo", "", "bar", "~/", "qux", "1", "-", "baz", ""))
  }

  test("distance should return distance") {
    val Right(p1) = Pointer.parse("123#")
    val Right(p2) = Pointer.parse("0/foo")
    val Left(r1) = p1.toSubtype
    val Left(r2) = p2.toSubtype

    assertEquals(r1.distance, 123)
    assertEquals(r2.distance, 0)
  }

  test("remainder should return the absolute part of a relative pointer") {
    val Right(p1) = Pointer.parse("123#")
    val Right(p2) = Pointer.parse("0/foo")
    val Left(r1) = p1.toSubtype
    val Left(r2) = p2.toSubtype

    val Right(remainder) = Pointer.Absolute.parse("/foo")

    assertEquals(r1.remainder, None)
    assertEquals(r2.remainder, Some(remainder))
  }

  test("toString should return the original input") {
    val examples = Vector(
      "/foo//bar/~0~1/qux/1/-/baz/",
      "",
      "/foo",
      "/foo/0",
      "/",
      "/a~1b",
      "/c%d",
      "/e^f",
      "/g|h",
      "/i\\j",
      "/k\"l",
      "/ ",
      "/m~0n",
      "0",
      "1/0",
      "2/highly/nested/objects",
      "0#",
      "1#"
    )

    examples.foreach { input =>
      val Right(p) = Pointer.parse(input)

      assertEquals(p.toString, input)
    }
  }
}
