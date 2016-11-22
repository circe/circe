package io.circe.testing

import cats.Eq
import cats.instances.either._
import cats.laws._
import cats.laws.discipline._
import io.circe.{ Decoder, Encoder, HCursor }
import io.circe.ast.Json
import org.scalacheck.Arbitrary
import org.scalacheck.Prop
import org.typelevel.discipline.Laws

trait CodecLaws[A] {
  def decode: Decoder[A]
  def encode: Encoder[A]

  def codecRoundTrip(a: A): IsEq[Decoder.Result[A]] =
    decode.decodeJson(encode(a)) <-> Right(a)

  def codecAccumulatingConsistency(json: Json): IsEq[Decoder.Result[A]] =
    decode(HCursor.fromJson(json)) <-> decode.accumulating(HCursor.fromJson(json)).leftMap(_.head).toEither
}

object CodecLaws {
  def apply[A](implicit d: Decoder[A], e: Encoder[A]): CodecLaws[A] = new CodecLaws[A] {
    val decode: Decoder[A] = d
    val encode: Encoder[A] = e
  }
}

trait CodecTests[A] extends Laws with ArbitraryInstances {
  def laws: CodecLaws[A]

  def codec(implicit A: Arbitrary[A], eq: Eq[A]): RuleSet = new DefaultRuleSet(
    name = "codec",
    parent = None,
    "roundTrip" -> Prop.forAll { (a: A) =>
      laws.codecRoundTrip(a)
    },
    "consistency with accumulating" -> Prop.forAll { (json: Json) =>
      laws.codecAccumulatingConsistency(json)
    }
  )
}

object CodecTests {
  def apply[A: Decoder: Encoder]: CodecTests[A] = new CodecTests[A] {
    val laws: CodecLaws[A] = CodecLaws[A]
  }
}
