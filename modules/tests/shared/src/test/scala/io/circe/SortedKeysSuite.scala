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

package io.circe

import io.circe.tests.PrinterSuite
import org.scalacheck.Prop._

trait SortedKeysSuite { this: PrinterSuite =>
  test("Printer with sortKeys should sort the object keys (example)") {
    val input = Json.obj(
      "one" -> Json.fromInt(1),
      "two" -> Json.fromInt(2),
      "three" -> Json.fromInt(3)
    )

    parser.parse(circePrinter.print(input)).toOption.flatMap(_.asObject) match {
      case None         => fail("Cannot parse result back to an object")
      case Some(output) =>
        assertEquals(output.keys.toList, List("one", "three", "two"))
    }
  }

  property("Printer with sortKeys should sort the object keys") {
    forAll { (value: Map[String, List[Int]]) =>
      val printed = circePrinter.print(implicitly[Encoder[Map[String, List[Int]]]].apply(value))
      val parsed = TestParser.parser.parse(printed).toOption.flatMap(_.asObject).get
      val keys = parsed.keys.toVector
      keys.sorted =? keys
    }
  }

  test("Sorting keys should handle \"\" consistently") {
    // From https://github.com/circe/circe/issues/1911
    val testMap: Map[String, List[Int]] = Map("4" -> Nil, "" -> Nil)

    val printed: String = circePrinter.print(Encoder[Map[String, List[Int]]].apply(testMap))

    TestParser.parser.parse(printed) match {
      case Left(e)      => fail(e.getLocalizedMessage, e)
      case Right(value) =>
        value.asObject.fold(fail(s"Expected object, but got ${value}.")) { value =>
          val keys: Vector[String] = value.keys.toVector
          assertEquals(keys.sorted, keys)
        }
    }
  }
}
