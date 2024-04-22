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

package io.circe.jawn

import munit.FunSuite

class JawnParserSuite extends FunSuite {
  test("JawnParser should respect maxValueSize for numbers") {
    val parser = JawnParser(10)

    assert(parser.parse("1000000000").isRight)
    assert(parser.parse("[1000000000]").isRight)
    assert(parser.parse("""{ "foo": 1000000000 }""").isRight)

    assert(parser.parse("10000000000").isLeft)
    assert(parser.parse("[10000000000]").isLeft)
    assert(parser.parse("""{ "foo": 10000000000 }""").isLeft)
  }
  test("JawnParser should respect maxValueSize for strings") {
    val parser = JawnParser(10)

    assert(parser.parse("\"1000000000\"").isRight)
    assert(parser.parse("[\"1000000000\"]").isRight)
    assert(parser.parse("""{ "foo": "1000000000" }""").isRight)

    assert(parser.parse("\"10000000000\"").isLeft)
    assert(parser.parse("[\"10000000000\"]").isLeft)
    assert(parser.parse("""{ "foo": "10000000000" }""").isLeft)
  }

  test("JawnParser should respect maxValueSize for object keys") {
    val parser = JawnParser(10)

    assert(parser.parse("""{ "1000000000": "foo" }""").isRight)
    assert(parser.parse("""{ "10000000000": "foo" }""").isLeft)
  }

  test("JawnParser should respect allowedDuplicateKeys") {
    assert(JawnParser(allowDuplicateKeys = true).parse("""{ "a": "b", "a": "c" }""").isRight)
    assert(JawnParser(allowDuplicateKeys = false).parse("""{ "a": "b", "a": "c" }""").isLeft)
  }
}
