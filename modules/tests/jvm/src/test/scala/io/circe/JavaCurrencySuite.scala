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

package io.circe

import cats._
import io.circe.testing.CodecTests
import io.circe.tests.CirceMunitSuite
import java.util.Currency
import org.scalacheck._
import scala.collection.JavaConverters._

final class JavaCurrencySuite extends CirceMunitSuite {
  import JavaCurrencySuite._

  checkAll("Codec[Currency]", CodecTests[Currency].codec)
}

object JavaCurrencySuite {

  lazy val availableCurrencies: Set[Currency] =
    Currency.getAvailableCurrencies.asScala.toSet

  implicit lazy val arbitraryCurrency: Arbitrary[Currency] =
    Arbitrary(Gen.oneOf(availableCurrencies))

  // Orphans
  implicit private lazy val eqInstance: Eq[Currency] =
    Eq.fromUniversalEquals
}
