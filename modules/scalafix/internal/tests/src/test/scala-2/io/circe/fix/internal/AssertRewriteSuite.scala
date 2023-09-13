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

package io.circe.internal.fix

import org.scalatest.funsuite.AnyFunSuiteLike
import scalafix.testkit._

final class AssertRewriteSuite extends AbstractSyntacticRuleSuite with AnyFunSuiteLike {
  val original0: String =
    """object Tests {
      |property("property") { forAll(a =>
      |  assert(actual == expected)
      |)
      |}}""".stripMargin

  check(
    _root_.io.circe.internal.fix.AssertRewrite.instance,
    "property",
    original0,
    """object Tests {
      |property("property") { forAll(a =>
      |  actual ?= expected
      |)
      |}}""".stripMargin
  )

  val original1: String =
    """object Tests {
      |test("test") {
      |  assert(actual == expected)
      |}}""".stripMargin

  check(
    _root_.io.circe.internal.fix.AssertRewrite.instance,
    "test",
    original1,
    """object Tests {
      |test("test") {
      |  assertEquals(actual, expected)
      |}}""".stripMargin
  )

  val original2: String =
    """object Tests {
      |property("test") { forAll(a =>
      |  assertEquals(actual, expected)
      |)
      |}}""".stripMargin

  check(
    _root_.io.circe.internal.fix.AssertRewrite.instance,
    "property with assertEquals",
    original2,
    """object Tests {
      |property("test") { forAll(a =>
      |  actual ?= expected
      |)
      |}}""".stripMargin
  )

  val original3: String =
    """object Tests {property("An hlist decoder should decode an array as an hlist")(hlistDecodeArrayProp)
      |  lazy val hlistDecodeArrayProp = forAll { (foo: String, bar: Int, baz: List[Char]) =>
      |    val expected = foo :: bar :: baz :: HNil
      |    val result = hlistDecoder.decodeJson(json)
      |
      |    assert(result === Right(expected))
      |  }}""".stripMargin

  check(
    _root_.io.circe.internal.fix.AssertRewrite.instance,
    "property with separate forAll",
    original3,
    """object Tests {property("An hlist decoder should decode an array as an hlist")(hlistDecodeArrayProp)
      |  lazy val hlistDecodeArrayProp = forAll { (foo: String, bar: Int, baz: List[Char]) =>
      |    val expected = foo :: bar :: baz :: HNil
      |    val result = hlistDecoder.decodeJson(json)
      |
      |    result ?= Right(expected)
      |  }}""".stripMargin
  )
}
