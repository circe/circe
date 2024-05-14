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

import scala.io.Source
import scala.util.Random

trait Spaces2PrinterExample { this: Spaces2PrinterSuite =>
  val rand: Random = new Random(0L)

  val doc: Json = (0 to 150).foldLeft(Json.obj()) {
    case (acc, i) if i % 3 == 0 =>
      val count = rand.nextInt(10)
      val strings = List.fill(count)(Json.fromString(rand.nextString(count)))
      val doubles = List.fill(count)(Json.fromDouble(rand.nextDouble())).flatten

      Json.obj(i.toString -> acc, "data" -> Json.fromValues(strings ++ doubles))
    case (acc, i) if i % 3 == 1 => Json.obj(i.toString -> acc, "data" -> Json.True)
    case (acc, i)               => Json.obj(i.toString -> acc)
  }

  val source = Source.fromInputStream(getClass.getResourceAsStream("/io/circe/spaces2-example.json"))
  val expected = source.mkString
  source.close()

  test("Printer.spaces2 should generate the expected output for the example doc") {
    val printed = Printer.spaces2.print(doc) + "\n"

    assertEquals(printed, expected)
  }
}
