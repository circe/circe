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

package io.circe.numbers.testing

import org.scalacheck.Arbitrary
import org.scalacheck.Gen

/**
 * An integral string with an optional leading minus sign and between 1 and 25
 * digits (inclusive).
 */
final case class IntegralString(value: String)

object IntegralString {
  implicit val arbitraryIntegralString: Arbitrary[IntegralString] = Arbitrary(
    for {
      sign <- Gen.oneOf("", "-")
      nonZero <- Gen.choose(1, 9).map(_.toString)

      /**
       * We want between 1 and 25 digits, with extra weight on the numbers of
       * digits around the size of `Long.MaxValue`.
       */
      count <- Gen.chooseNum(0, 24, 17, 18, 19)
      rest <- Gen.buildableOfN[String, Char](count, Gen.numChar)
    } yield IntegralString(s"$sign$nonZero$rest")
  )
}
