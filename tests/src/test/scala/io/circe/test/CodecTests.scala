package io.circe.test

import algebra.Eq
import cats.data.Xor
import cats.laws._
import cats.laws.discipline._
import io.circe.Cats._
import io.circe.{ Decoder, DecodingFailure, Encoder }
import org.scalacheck.Arbitrary
import org.scalacheck.Prop
import org.typelevel.discipline.Laws

trait CodecLaws[A] {
  def decode: Decoder[A]
  def encode: Encoder[A]

  def codecRoundTrip(a: A): IsEq[Xor[DecodingFailure, A]] =
    encode(a).as(decode) <-> Xor.right(a)
}

object CodecLaws {
  def apply[A](implicit d: Decoder[A], e: Encoder[A]): CodecLaws[A] = new CodecLaws[A] {
    val decode: Decoder[A] = d
    val encode: Encoder[A] = e
  }
}

trait CodecTests[A] extends Laws {
  def laws: CodecLaws[A]

  def codec(implicit A: Arbitrary[A], eq: Eq[A]): RuleSet = new DefaultRuleSet(
    name = "codec",
    parent = None,
    "roundTrip" -> Prop.forAll { (a: A) =>
      laws.codecRoundTrip(a)
    }
  )
}

object CodecTests {
  def apply[A: Decoder: Encoder]: CodecTests[A] = new CodecTests[A] {
    val laws: CodecLaws[A] = CodecLaws[A]
  }
}
