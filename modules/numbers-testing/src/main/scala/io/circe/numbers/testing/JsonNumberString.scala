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

package io.circe.numbers.testing

import org.scalacheck.Arbitrary
import org.scalacheck.Gen

/**
 * An arbitrary JSON number, represented as a string.
 */
final case class JsonNumberString(value: String)

object JsonNumberString {
  private val genSign: Gen[String] = Gen.oneOf("", "-")
  private val genIntegral: Gen[String] = Gen.oneOf(
    Gen.const("0"),
    for {
      nonZero <- Gen.choose(1, 9).map(_.toString)
      rest <- Gen.numStr
    } yield s"$nonZero$rest"
  )
  private val genFractional: Gen[String] =
    Gen.nonEmptyListOf(Gen.numChar).map(_.mkString).map("." + _)
  private val genExponent: Gen[String] =
    for {
      e <- Gen.oneOf("e", "E")
      s <- Gen.oneOf("", "+", "-")
      n <- Gen.nonEmptyListOf(Gen.numChar).map(_.mkString)
    } yield s"$e$s$n"

  implicit val arbitraryJsonNumberString: Arbitrary[JsonNumberString] = Arbitrary(
    Gen.frequency(
      1 -> (for {
        sign <- genSign
        exponent <- genExponent
      } yield JsonNumberString(s"${sign}0$exponent")),
      10 -> (for {
        sign <- genSign
        integral <- genIntegral
        fractional <- Gen.oneOf(Gen.const(""), genFractional)
        exponent <- Gen.oneOf(Gen.const(""), genExponent)
      } yield JsonNumberString(s"$sign$integral$fractional$exponent"))
    )
  )
}
