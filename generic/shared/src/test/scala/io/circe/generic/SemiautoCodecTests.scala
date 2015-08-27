package io.circe.generic

import cats.data.Xor
import io.circe.{ Decoder, Encoder, Json }
import io.circe.generic.semiauto._
import io.circe.test.{ CodecTests, CirceSuite }
import org.scalacheck.Prop.forAll
import shapeless.CNil

class SemiautoCodecTests extends CirceSuite with Examples {
  implicit def decodeQux[A: Decoder]: Decoder[Qux[A]] = deriveFor[Qux[A]].decoder
  implicit def encodeQux[A: Encoder]: Encoder[Qux[A]] = deriveFor[Qux[A]].encoder
  implicit val decodeWub: Decoder[Wub] = deriveFor[Wub].decoder
  implicit val encodeWub: Encoder[Wub] = deriveFor[Wub].encoder
  implicit val decodeFoo: Decoder[Foo] = deriveFor[Foo].decoder
  implicit val encodeFoo: Encoder[Foo] = deriveFor[Foo].encoder

  checkAll("Codec[Tuple1[Int]]", CodecTests[Tuple1[Int]].codec)
  checkAll("Codec[(Int, Int, Foo)]", CodecTests[(Int, Int, Foo)].codec)
  checkAll("Codec[Qux[Int]]", CodecTests[Qux[Int]].codec)
  checkAll("Codec[Foo]", CodecTests[Foo].codec)

  test("Generic instances should not interfere with base instances") {
    check {
      forAll { (is: List[Int]) =>
        val json = Encoder[List[Int]].apply(is)

        json === Json.fromValues(is.map(Json.int)) && json.as[List[Int]] === Xor.right(is)
      }
    }
  }
}
