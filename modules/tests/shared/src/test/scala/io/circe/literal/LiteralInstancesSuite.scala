package io.circe.literal

import cats.Eq
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

  "A literal String codec" should "round-trip values" in {
    val w = Witness("foo")

    assert(Decoder[w.T].decodeJson(Encoder[w.T].apply(w.value)) === Right(w.value))
  }

  "A literal Boolean codec" should "round-trip values" in {
    val w = Witness(true)

    assert(Decoder[w.T].decodeJson(Encoder[w.T].apply(w.value)) === Right(w.value))
  }

  "A literal Float codec" should "round-trip values" in {
    val w = Witness(0.0F)

    assert(Decoder[w.T].decodeJson(Encoder[w.T].apply(w.value)) === Right(w.value))
  }

  "A literal Double codec" should "round-trip values" in {
    val w = Witness(0.0)

    assert(Decoder[w.T].decodeJson(Encoder[w.T].apply(w.value)) === Right(w.value))
  }

  "A literal Char codec" should "round-trip values" in {
    val w = Witness('a')

    assert(Decoder[w.T].decodeJson(Encoder[w.T].apply(w.value)) === Right(w.value))
  }

  "A literal Int codec" should "round-trip values" in {
    val w = Witness(0)

    assert(Decoder[w.T].decodeJson(Encoder[w.T].apply(w.value)) === Right(w.value))
  }

  "A literal Long codec" should "round-trip values" in {
    val w = Witness(0L)

    assert(Decoder[w.T].decodeJson(Encoder[w.T].apply(w.value)) === Right(w.value))
  }
}
