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

package io.circe.parser

import io.circe.Json
import io.circe.testing.ParserTests
import io.circe.tests.CirceMunitSuite
import org.scalacheck.Prop._

class ParserSuite extends CirceMunitSuite {
  checkAll("Parser", ParserTests(`package`).fromString)

  property("parse and decode(Accumulating) should fail on invalid input") {
    forAll { (s: String) =>
      assert(parse(s"Not JSON $s").isLeft)
      assert(decode[Json](s"Not JSON $s").isLeft)
      assert(decodeAccumulating[Json](s"Not JSON $s").isInvalid)
    }
  }
}
