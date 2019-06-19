package io.circe.scodec

import cats.kernel.Eq
import io.circe.{ Decoder, Json }
import io.circe.parser.parse
import io.circe.testing.CodecTests
import io.circe.tests.CirceSuite
import org.scalacheck.Arbitrary
import org.scalatest.Matchers
import org.scalatest.matchers.{ MatchResult, Matcher }
import _root_.scodec.bits._

class ScodecSuite extends CirceSuite with Matchers with BitVectorMatchers {
  implicit val arbitraryBitVector: Arbitrary[BitVector] =
    Arbitrary(Arbitrary.arbitrary[Iterable[Boolean]].map(BitVector.bits))

  implicit val arbitraryByteVector: Arbitrary[ByteVector] =
    Arbitrary(Arbitrary.arbitrary[Array[Byte]].map(ByteVector.view))

  implicit val eqBitVector: Eq[BitVector] = Eq.fromUniversalEquals
  implicit val eqByteVector: Eq[ByteVector] = Eq.fromUniversalEquals

  checkLaws("Codec[BitVector]", CodecTests[BitVector].codec)
  checkLaws("Codec[ByteVector]", CodecTests[ByteVector].codec)

  "Codec[ByteVector]" should "return failure for Json String" in {
    val json = Json.fromString("mA==")
    assert(decodeBitVector.decodeJson(json).isLeft)
  }

  "Codec[ByteVector]" should "return failure in case input is an incomplete object" in {
    decodeBitVector should failFor("{}")
    decodeBitVector should failFor("""{"bits": "mA=="}""")
    decodeBitVector should failFor("""{"length": 6}""")
  }

  // this test shows that decoder is to some extend liberal
  // even though such input could not have been produced by BitVector encoder it's getting decoded to BitVector
  "Codec[ByteVector]" should "return empty BitVector in case contains only non-zero header" in {
    decodeBitVector should decodeTo("""{"bits": "mA==", "length": 8}""", bin"10011000")
    decodeBitVector should decodeTo("""{"bits": "mA==", "length": 16}""", bin"10011000")
    decodeBitVector should decodeTo("""{"bits": "mA==", "length": 0}""", BitVector.empty)
  }
}

trait BitVectorMatchers {
  class FailFor(input: String) extends Matcher[Decoder[BitVector]] {
    override def apply(decoder: Decoder[BitVector]): MatchResult = {
      val Right(json) = parse(input)
      MatchResult(decoder.decodeJson(json).isLeft, s"Has not failed for $input", s"Failed for $input")
    }
  }

  def failFor(input: String): FailFor = new FailFor(input)

  class DecodeTo(input: String, expectedBitVector: BitVector) extends Matcher[Decoder[BitVector]] {
    override def apply(decoder: Decoder[BitVector]): MatchResult = {
      val Right(json) = parse(input)

      val decoded = decoder.decodeJson(json)
      val expected = Right(expectedBitVector)
      MatchResult(
        decoded == Right(expectedBitVector),
        s"Decoded to [$decoded], expected: [$expected]",
        s"Decoded to [$expected]"
      )
    }
  }

  def decodeTo(input: String, expectedBitVector: BitVector): DecodeTo = new DecodeTo(input, expectedBitVector)
}
