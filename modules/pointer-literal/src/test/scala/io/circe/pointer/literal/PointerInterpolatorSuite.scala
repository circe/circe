package io.circe.pointer.literal

import io.circe.pointer._
import munit.ScalaCheckSuite
import org.scalacheck._

final class PointerInterpolatorSuite extends ScalaCheckSuite {
  test("The pointer string interpolater should parse valid absolute JSON pointers") {
    val inputs = List("", "/foo", "/foo/0", "/", "/a~1b", "/c%d", "/e^f", "/g|h", "/i\\j", "/k\"l", "/ ", "/m~0n")
    val values = List(
      pointer"",
      pointer"/foo",
      pointer"/foo/0",
      pointer"/",
      pointer"/a~1b",
      pointer"/c%d",
      pointer"/e^f",
      pointer"/g|h",
      pointer"""/i\j""",
      pointer"""/k"l""",
      pointer"/ ",
      pointer"/m~0n"
    )

    values.zip(inputs).foreach {
      case (value, input) =>
        assertEquals(Pointer.parse(input), Right(value))
    }
  }

  test("The pointer string interpolater should parse valid relative JSON pointers") {
    val inputs = List("0", "1/0", "2/highly/nested/objects", "0#", "1#")
    val values = List(pointer"0", pointer"1/0", pointer"2/highly/nested/objects", pointer"0#", pointer"1#")

    values.zip(inputs).foreach {
      case (value, input) =>
        assertEquals(Pointer.parse(input), Right(value))
    }
  }

  test("The pointer string interpolater should work with interpolated values that need escaping") {
    val s = "foo~bar/baz/~"
    val Right(expected) = Pointer.parse("/base/foo~0bar~1baz~1~0/leaf")
    assertEquals(pointer"/base/$s/leaf", expected)
  }

  property("The pointer string interpolater should work with arbitrary interpolated strings") {
    Prop.forAll(ScalaCheckInstances.genPointerReferenceString) { (v: String) =>
      val Right(expected) = Pointer.parse(s"/foo/$v/bar")

      pointer"/foo/$v/bar" == expected
    }
  }

  property("The pointer string interpolater should work with arbitrary interpolated integers") {
    Prop.forAll { (v: Long) =>
      val Right(expected) = Pointer.parse(s"/foo/$v/bar")

      pointer"/foo/$v/bar" == expected
    }
  }
}
