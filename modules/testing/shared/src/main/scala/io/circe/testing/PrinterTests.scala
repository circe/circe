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

package io.circe.testing

import cats.kernel.Eq
import cats.kernel.laws.SerializableLaws
import cats.laws._
import cats.laws.discipline._
import io.circe.Decoder
import io.circe.Encoder
import io.circe.Parser
import io.circe.Printer
import org.scalacheck.Arbitrary
import org.scalacheck.Prop
import org.scalacheck.Shrink
import org.typelevel.discipline.Laws

trait PrinterLaws[A] {
  def decode: Decoder[A]
  def encode: Encoder[A]

  def printerRoundTrip(printer: Printer, parser: Parser, a: A): IsEq[Option[A]] =
    parser.decode(printer.print(encode(a)))(decode).toOption <-> Some(a)
}

object PrinterLaws {
  def apply[A](implicit decodeA: Decoder[A], encodeA: Encoder[A]): PrinterLaws[A] =
    new PrinterLaws[A] {
      val decode: Decoder[A] = decodeA
      val encode: Encoder[A] = encodeA
    }
}

trait PrinterTests[A] extends Laws {
  def laws: PrinterLaws[A]

  def printer(printer: Printer, parser: Parser)(implicit
    arbitraryA: Arbitrary[A],
    shrinkA: Shrink[A],
    eqA: Eq[A]
  ): RuleSet =
    new DefaultRuleSet(
      name = "printer",
      parent = None,
      "roundTrip" -> Prop.forAll { (a: A) =>
        laws.printerRoundTrip(printer, parser, a)
      },
      "printer serializability" -> SerializableLaws.serializable(printer)
    )
}

object PrinterTests {
  def apply[A](implicit decodeA: Decoder[A], encodeA: Encoder[A]): PrinterTests[A] =
    new PrinterTests[A] {
      val laws: PrinterLaws[A] = PrinterLaws[A]
    }
}
