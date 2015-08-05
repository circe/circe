package io.jfc.test

import algebra.Eq
import cats.data.Xor
import cats.laws._
import cats.laws.discipline._
import io.jfc.{ Decode, DecodeFailure, Encode }
import org.scalacheck.Arbitrary
import org.scalacheck.Prop
import org.typelevel.discipline.Laws

/**
 * 
 */
trait CodecLaws[A] {
  def decode: Decode[A]
  def encode: Encode[A]

  def codecRoundTrip(a: A): IsEq[Xor[DecodeFailure, A]] =
    encode(a).as(decode) <-> Xor.right(a)
}

object CodecLaws {
  def apply[A](implicit d: Decode[A], e: Encode[A]): CodecLaws[A] = new CodecLaws[A] {
    val decode: Decode[A] = d
    val encode: Encode[A] = e
  }
}

trait CodecTests[A] extends Laws {
  def laws: CodecLaws[A]

  def codec(implicit A: Arbitrary[A], eq: Eq[A]): RuleSet = new DefaultRuleSet(
    name = "codec",
    parent = None,
    "roundTrip" -> Prop.forAll { (a: A) =>
      val result: IsEq[Xor[DecodeFailure, A]] = laws.codecRoundTrip(a)
      if (result.lhs != result.rhs) println(result)
      result
    }
  )
}

object CodecTests {
  def apply[A: Decode: Encode]: CodecTests[A] = new CodecTests[A] {
    val laws: CodecLaws[A] = CodecLaws[A]
  }
}
