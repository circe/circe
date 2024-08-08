/*
 * Copyright 2024 circe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.circe.pointer.literal

import io.circe.pointer._
import munit.ScalaCheckSuite
import org.scalacheck.Prop._
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
    val s: String = "foo~bar/baz/~"
    assertEquals(Pointer.parse("/base/foo~0bar~1baz~1~0/leaf"), Right(pointer"/base/$s/leaf"))
  }

  test("The pointer string interpolater should work with interpolated values that are already escaped") {
    val s: String = "foo~0bar~1baz~1~0"
    assertEquals(Pointer.parse("/base/foo~0bar~1baz~1~0/leaf"), Right(pointer"/base/$s/leaf"))
  }

  property("The pointer string interpolater should work with arbitrary interpolated strings") {
    Prop.forAll(ScalaCheckInstances.genPointerReferenceString) { (v: String) =>
      Pointer.parse(s"/foo/$v/bar") ?= Right(pointer"/foo/$v/bar")
    }
  }

  property("The pointer string interpolater should work with arbitrary interpolated integers") {
    Prop.forAll { (v: Long) =>
      Pointer.parse(s"/foo/$v/bar") ?= Right(pointer"/foo/$v/bar")
    }
  }
}
