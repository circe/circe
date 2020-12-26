package io.circe.scodec

import cats.kernel.Eq
import cats.syntax.eq._
import io.circe.Json
import io.circe.parser.decode
import io.circe.testing.CodecTests
import io.circe.tests.CirceMunitSuite
import org.scalacheck.Arbitrary
import _root_.scodec.bits._

class ScodecSuite extends CirceMunitSuite {
  implicit val arbitraryBitVector: Arbitrary[BitVector] =
    Arbitrary(Arbitrary.arbitrary[Iterable[Boolean]].map(BitVector.bits))

  implicit val arbitraryByteVector: Arbitrary[ByteVector] =
    Arbitrary(Arbitrary.arbitrary[Array[Byte]].map(ByteVector.view))

  implicit val eqBitVector: Eq[BitVector] = Eq.fromUniversalEquals
  implicit val eqByteVector: Eq[ByteVector] = Eq.fromUniversalEquals

  checkAll("Codec[BitVector]", CodecTests[BitVector].codec)
  checkAll("Codec[ByteVector]", CodecTests[ByteVector].codec)

  test("Codec[ByteVector] should return failure for Json String") {
    val json = Json.fromString("mA==")
    assert(decodeBitVector.decodeJson(json).isLeft)
  }

  test("Codec[ByteVector] should return failure in case input is an incomplete object") {
    assert(decode("{}")(decodeBitVector).isLeft)
    assert(decode("""{"bits": "mA=="}""")(decodeBitVector).isLeft)
    assert(decode("""{"length": 6}""")(decodeBitVector).isLeft)
  }

  // this test shows that decoder is to some extend liberal
  // even though such input could not have been produced by BitVector encoder it's getting decoded to BitVector
  test("Codec[ByteVector] should return empty BitVector in case contains only non-zero header") {
    assert(decode("""{"bits": "mA==", "length": 8}""")(decodeBitVector) === Right(bin"10011000"))
    assert(decode("""{"bits": "mA==", "length": 16}""")(decodeBitVector) === Right(bin"10011000"))
    assert(decode("""{"bits": "mA==", "length": 0}""")(decodeBitVector) === Right(BitVector.empty))
  }
}
