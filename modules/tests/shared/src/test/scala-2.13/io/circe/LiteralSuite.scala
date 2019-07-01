package io.circe

import cats.kernel.Eq
import io.circe.testing.CodecTests
import io.circe.tests.CirceSuite
import org.scalacheck.{ Arbitrary, Gen }

class LiteralCodecSuite extends CirceSuite {
  implicit def arbitraryLiteral[L](implicit L: ValueOf[L]): Arbitrary[L] = Arbitrary(Gen.const(L.value))
  implicit def eqLiteral[A, L <: A](implicit A: Eq[A], L: ValueOf[L]): Eq[L] = Eq.by[L, A](identity)

  checkLaws("""Codec["foo"]"""", CodecTests["foo"].codec)
  checkLaws("""Codec[1.2345]""", CodecTests[1.2345].codec)
  checkLaws("""Codec[1.234F]""", CodecTests[1.2345f].codec)
  checkLaws("""Codec[12345L]""", CodecTests[12345L].codec)
  checkLaws("""Codec[123456]""", CodecTests[123456].codec)
  checkLaws("""Codec['a']""", CodecTests['a'].codec)
  checkLaws("""Codec[true]""", CodecTests[true].codec)
  checkLaws("""Codec[false]""", CodecTests[false].codec)
}
