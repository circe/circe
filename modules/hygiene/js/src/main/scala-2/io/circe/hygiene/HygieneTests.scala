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

package io.circe.hygiene

import io.circe.generic.auto._
import io.circe.syntax._

sealed trait Foo
case class Bar(xs: scala.List[java.lang.String]) extends Foo
case class Qux(i: scala.Int, d: scala.Option[scala.Double]) extends Foo

/**
 * Compilation tests for macro hygiene.
 *
 * Fake definitions suggested by Jason Zaugg.
 */
object HygieneTests {
  val foo: Foo = Bar(scala.List("abc", "def", "ghi"))
  val json = foo.asJson.noSpaces
}
