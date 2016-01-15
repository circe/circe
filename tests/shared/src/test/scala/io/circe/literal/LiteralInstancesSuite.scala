package io.circe.literal

import algebra.Eq
import cats.data.Xor
import io.circe.{ Decoder, Encoder }
import io.circe.tests.CirceSuite
import shapeless.Witness

class LiteralInstancesSuite extends CirceSuite {
  def eqSingleton[S <: V with Singleton, V: Eq]: Eq[S] = Eq.by[S, V](identity)

  implicit def eqSingletonString[S <: String with Singleton]: Eq[S] = eqSingleton[S, String]
  implicit def eqSingletonBoolean[S <: Boolean with Singleton]: Eq[S] = eqSingleton[S, Boolean]
  implicit def eqSingletonDouble[S <: Double with Singleton]: Eq[S] = eqSingleton[S, Double]
  implicit def eqSingletonFloat[S <: Float with Singleton]: Eq[S] = eqSingleton[S, Float]
  implicit def eqSingletonLong[S <: Long with Singleton]: Eq[S] = eqSingleton[S, Long]
  implicit def eqSingletonInt[S <: Int with Singleton]: Eq[S] = eqSingleton[S, Int]
  implicit def eqSingletonChar[S <: Char with Singleton]: Eq[S] = eqSingleton[S, Char]

  test("Literal string encoding and decoding") {
    val w = Witness("foo")

    assert(Decoder[w.T].apply(Encoder[w.T].apply(w.value).hcursor) === Xor.right(w.value))
  }

  test("Literal boolean encoding and decoding") {
    val w = Witness(true)

    assert(Decoder[w.T].apply(Encoder[w.T].apply(w.value).hcursor) === Xor.right(w.value))
  }

  test("Literal double encoding and decoding") {
    val w = Witness(0.0)

    assert(Decoder[w.T].apply(Encoder[w.T].apply(w.value).hcursor) === Xor.right(w.value))
  }

  test("Literal float encoding and decoding") {
    val w = Witness(0.0F)

    assert(Decoder[w.T].apply(Encoder[w.T].apply(w.value).hcursor) === Xor.right(w.value))
  }

  test("Literal long encoding and decoding") {
    val w = Witness(0L)

    assert(Decoder[w.T].apply(Encoder[w.T].apply(w.value).hcursor) === Xor.right(w.value))
  }

  test("Literal int encoding and decoding") {
    val w = Witness(0)

    assert(Decoder[w.T].apply(Encoder[w.T].apply(w.value).hcursor) === Xor.right(w.value))
  }

  test("Literal char encoding and decoding") {
    val w = Witness('a')

    assert(Decoder[w.T].apply(Encoder[w.T].apply(w.value).hcursor) === Xor.right(w.value))
  }
}
