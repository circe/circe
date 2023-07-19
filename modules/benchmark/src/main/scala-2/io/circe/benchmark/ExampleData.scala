/*
 * Copyright 2023 circe
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

package io.circe.benchmark

import io.circe.Json
import io.circe.syntax._

class ExampleData {
  val ints: List[Int] = (0 to 1000).toList
  val booleans: List[Boolean] = ints.map(_ % 2 == 0)

  val foos: Map[String, Foo] = List
    .tabulate(100) { i =>
      ("b" * i) -> Foo("a" * i, (i + 2.0) / (i + 1.0), i, i * 1000L, (0 to i).map(_ % 2 == 0).toList)
    }
    .toMap

  val intsJson: Json = ints.asJson
  val booleansJson: Json = booleans.asJson
  val foosJson: Json = foos.asJson
  val helloWorldJson: Json = Json.obj("message" -> Json.fromString("Hello, World!"))
}
