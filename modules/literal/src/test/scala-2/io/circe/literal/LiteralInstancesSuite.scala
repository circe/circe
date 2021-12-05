package io.circe.literal

import io.circe.{ Decoder, Encoder }
import munit.FunSuite

import shapeless.Witness

class LiteralInstancesSuite extends FunSuite {
  test("A literal String codec should round-trip values") {
    val w = Witness("foo")

    assertEquals(Decoder[w.T].apply(Encoder[w.T].apply(w.value).hcursor), Right(w.value))
  }

  test("A literal Double codec should round-trip values") {
    val w = Witness(0.0)

    assertEquals(Decoder[w.T].apply(Encoder[w.T].apply(w.value).hcursor), Right(w.value))
  }

  test("A literal Float codec should round-trip values") {
    val w = Witness(0.0f)

    assertEquals(Decoder[w.T].apply(Encoder[w.T].apply(w.value).hcursor), Right(w.value))
  }

  test("A literal Long codec should round-trip values") {
    val w = Witness(0L)

    assertEquals(Decoder[w.T].apply(Encoder[w.T].apply(w.value).hcursor), Right(w.value))
  }

  test("A literal Int codec should round-trip values") {
    val w = Witness(0)

    assertEquals(Decoder[w.T].apply(Encoder[w.T].apply(w.value).hcursor), Right(w.value))
  }

  test("A literal Char codec should round-trip values") {
    val w = Witness('a')

    assertEquals(Decoder[w.T].apply(Encoder[w.T].apply(w.value).hcursor), Right(w.value))
  }

  test("A literal Boolean codec should round-trip values") {
    val w = Witness(true)

    assertEquals(Decoder[w.T].apply(Encoder[w.T].apply(w.value).hcursor), Right(w.value))
  }
}

