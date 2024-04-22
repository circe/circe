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

package io.circe.tests.examples

import cats.kernel.Eq
import org.scalacheck.{ Arbitrary, Gen }

sealed trait CardinalDirection
case object North extends CardinalDirection
case object South extends CardinalDirection
case object East extends CardinalDirection
case object West extends CardinalDirection

object CardinalDirection {
  implicit val eqCardinalDirection: Eq[CardinalDirection] = Eq.fromUniversalEquals
  implicit val arbitraryCardinalDirection: Arbitrary[CardinalDirection] = Arbitrary(
    Gen.oneOf(North, South, East, West)
  )
}

sealed trait ExtendedCardinalDirection
case object North2 extends ExtendedCardinalDirection
case object South2 extends ExtendedCardinalDirection
case object East2 extends ExtendedCardinalDirection
case object West2 extends ExtendedCardinalDirection
case class NotACardinalDirectionAtAll(x: String) extends ExtendedCardinalDirection

object ExtendedCardinalDirection {
  implicit val eqExtendedCardinalDirection: Eq[ExtendedCardinalDirection] = Eq.fromUniversalEquals
  implicit val arbitraryExtendedCardinalDirection: Arbitrary[ExtendedCardinalDirection] = Arbitrary(
    Gen.oneOf(
      Gen.const(North2),
      Gen.const(South2),
      Gen.const(East2),
      Gen.const(West2),
      Arbitrary.arbitrary[String].map(NotACardinalDirectionAtAll(_))
    )
  )
}
