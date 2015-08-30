package io.circe.test

import algebra.Eq
import cats.data.Xor
import cats.laws._
import cats.laws.discipline._
import cats.std.option._
import io.circe.{ Decoder, Encoder, Parser, Printer }
import org.scalacheck.Arbitrary
import org.scalacheck.Prop
import org.typelevel.discipline.Laws

trait PrinterLaws[A] {
  def decode: Decoder[A]
  def encode: Encoder[A]

  def printerRoundTrip(printer: Printer, parser: Parser, a: A): IsEq[Option[A]] =
    parser.decode(printer.pretty(encode(a)))(decode).toOption <-> Some(a)
}

object PrinterLaws {
  def apply[A](implicit d: Decoder[A], e: Encoder[A]): PrinterLaws[A] =
    new PrinterLaws[A] {
      val decode: Decoder[A] = d
      val encode: Encoder[A] = e
    }
}

trait PrinterTests[A] extends Laws {
  def laws: PrinterLaws[A]

  def printer(printer: Printer, parser: Parser)(implicit
    A: Arbitrary[A],
    eq: Eq[A]
  ): RuleSet =
    new DefaultRuleSet(
      name = "printer",
      parent = None,
      "roundTrip" -> Prop.forAll { (a: A) =>
        isEqToProp(laws.printerRoundTrip(printer, parser, a))
      }
    )
}

object PrinterTests {
  def apply[A: Decoder: Encoder]: PrinterTests[A] =
    new PrinterTests[A] {
      val laws: PrinterLaws[A] = PrinterLaws[A]
    }
}
