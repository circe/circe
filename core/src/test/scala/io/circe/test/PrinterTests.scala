package io.circe.test

import algebra.Eq
import argonaut.{ DecodeJson, Parse }
import cats.data.Xor
import cats.laws._
import cats.laws.discipline._
import cats.std.option._
import io.circe.{ Decoder, DecodingFailure, Encoder, Printer }
import org.scalacheck.Arbitrary
import org.scalacheck.Prop
import org.typelevel.discipline.Laws

trait PrinterLaws[A] {
  def decode: DecodeJson[A]
  def encode: Encoder[A]

  def printerRoundTrip(printer: Printer, a: A): IsEq[Option[A]] =
    Parse.decodeOption(printer.pretty(encode(a)))(decode) <-> Some(a)
}

object PrinterLaws {
  def apply[A](implicit d: DecodeJson[A], e: Encoder[A]): PrinterLaws[A] =
    new PrinterLaws[A] {
      val decode: DecodeJson[A] = d
      val encode: Encoder[A] = e
    }
}

trait PrinterTests[A] extends Laws {
  def laws: PrinterLaws[A]

  def printer(printer: Printer)(implicit A: Arbitrary[A], eq: Eq[A]): RuleSet =
    new DefaultRuleSet(
      name = "printer",
      parent = None,
      "roundTrip" -> Prop.forAll { (a: A) =>
        isEqToProp(laws.printerRoundTrip(printer, a))
      }
    )
}

object PrinterTests {
  def apply[A: DecodeJson: Encoder]: PrinterTests[A] =
    new PrinterTests[A] {
      val laws: PrinterLaws[A] = PrinterLaws[A]
    }
}
