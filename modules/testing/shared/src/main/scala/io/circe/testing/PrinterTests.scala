package io.circe.testing

import cats.instances.option._
import cats.kernel.Eq
import cats.kernel.laws.SerializableLaws
import cats.laws._
import cats.laws.discipline._
import io.circe.{ Decoder, Encoder, Parser, Printer }
import org.scalacheck.{ Arbitrary, Prop, Shrink }
import org.typelevel.discipline.Laws

trait PrinterLaws[A] {
  def decode: Decoder[A]
  def encode: Encoder[A]

  def printerRoundTrip(printer: Printer, parser: Parser, a: A): IsEq[Option[A]] =
    parser.decode(printer.pretty(encode(a)))(decode).toOption <-> Some(a)
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

  def printer(printer: Printer, parser: Parser)(
    implicit
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
