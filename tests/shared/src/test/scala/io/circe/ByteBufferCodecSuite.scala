package io.circe

import java.nio.ByteBuffer

import algebra.Eq
import io.circe.tests.{ CodecTests, CirceSuite }
import org.scalacheck.{ Gen, Arbitrary }

class ByteBufferCodecSuite extends CirceSuite {

  implicit val byteBufferArbitrary: Arbitrary[ByteBuffer] = Arbitrary(
    Arbitrary.arbitrary[Array[Byte]].map(bytes =>
      ByteBuffer.wrap(bytes)
    )
  )

  implicit val base64CharactersArbitrary: Arbitrary[Char] = Arbitrary(
    //@see java.util.Base64.Encoder.toBase64
    Gen.oneOf(Seq(
      'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
      'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
      'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
      'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'
    ))
  )

  implicit val base64StringArbitrary: Arbitrary[String] = Arbitrary(
    Arbitrary.arbitrary[String].filter(_.length > 2)
  )

  implicit val byteBufferEq: Eq[ByteBuffer] = Eq.fromUniversalEquals

  checkAll("Codec[ByteBuffer]", CodecTests[ByteBuffer].codec)

}
