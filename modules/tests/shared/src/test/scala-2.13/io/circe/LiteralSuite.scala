package io.circe

import cats.kernel.Eq
import cats.kernel.instances.all._
import io.circe.testing.CodecTests
import io.circe.tests.CirceSuite
import org.scalacheck.{ Arbitrary, Gen }

class LiteralCodecSuite extends CirceSuite {
  implicit def arbitraryLiteral[L](implicit L: ValueOf[L]): Arbitrary[L] = Arbitrary(Gen.const(L.value))
  implicit def eqLiteral[A, L <: A](implicit A: Eq[A], L: ValueOf[L]): Eq[L] = Eq.by[L, A](identity)

  checkAll("""Codec["foo"]"""", CodecTests["foo"].codec)
  checkAll("""Codec[1.2345]""", CodecTests[1.2345].codec)
  checkAll("""Codec[1.234F]""", CodecTests[1.2345f].codec)
  checkAll("""Codec[12345L]""", CodecTests[12345L].codec)
  checkAll("""Codec[123456]""", CodecTests[123456].codec)
  checkAll("""Codec['a']""", CodecTests['a'].codec)
  checkAll("""Codec[true]""", CodecTests[true].codec)
  checkAll("""Codec[false]""", CodecTests[false].codec)
}
