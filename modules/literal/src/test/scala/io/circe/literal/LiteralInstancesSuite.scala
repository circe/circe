package io.circe.literal

import io.circe.{ Decoder, Encoder }
import org.scalatest.{ FunSpec, Matchers }
import shapeless.Witness

class LiteralInstancesSuite extends FunSpec with Matchers {
  describe("A literal String codec") {
    it("should round-trip values") {
      val w = Witness("foo")

      Decoder[w.T].apply(Encoder[w.T].apply(w.value).hcursor) shouldBe Right(w.value)
    }
  }

  describe("A literal Double codec") {
    it("should round-trip values") {
      val w = Witness(0.0)

      Decoder[w.T].apply(Encoder[w.T].apply(w.value).hcursor) shouldBe Right(w.value)
    }
  }

  describe("A literal Float codec") {
    it("should round-trip values") {
      val w = Witness(0.0f)

      Decoder[w.T].apply(Encoder[w.T].apply(w.value).hcursor) shouldBe Right(w.value)
    }
  }

  describe("A literal Long codec") {
    it("should round-trip values") {
      val w = Witness(0L)

      Decoder[w.T].apply(Encoder[w.T].apply(w.value).hcursor) shouldBe Right(w.value)
    }
  }

  describe("A literal Int codec") {
    it("should round-trip values") {
      val w = Witness(0)

      Decoder[w.T].apply(Encoder[w.T].apply(w.value).hcursor) shouldBe Right(w.value)
    }
  }

  describe("A literal Char codec") {
    it("should round-trip values") {
      val w = Witness('a')

      Decoder[w.T].apply(Encoder[w.T].apply(w.value).hcursor) shouldBe Right(w.value)
    }
  }

  describe("A literal Boolean codec") {
    it("should round-trip values") {
      val w = Witness(true)

      Decoder[w.T].apply(Encoder[w.T].apply(w.value).hcursor) shouldBe Right(w.value)
    }
  }
}
