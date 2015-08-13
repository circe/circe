package io.circe.generic

import cats.data.Xor
import cats.laws.discipline.eq._
import io.circe.{ Decoder, Encoder, Json }
import io.circe.generic.semiauto._
import io.circe.test.{ CodecTests, CirceSuite }
import org.scalacheck.Prop.forAll
import shapeless.CNil

class SemiautoCodecTests extends CirceSuite with Examples {
  import tuple._
  import incomplete._
  implicit def decodeQux[A: Decoder]: Decoder[Qux[A]] = deriveFor[Qux[A]].decoder
  implicit def encodeQux[A: Encoder]: Encoder[Qux[A]] = deriveFor[Qux[A]].encoder
  implicit val decodeWub: Decoder[Wub] = deriveFor[Wub].decoder
  implicit val encodeWub: Encoder[Wub] = deriveFor[Wub].encoder
  implicit val decodeFoo: Decoder[Foo] = deriveFor[Foo].decoder
  implicit val encodeFoo: Encoder[Foo] = deriveFor[Foo].encoder

  checkAll("Codec[Tuple1[Int]]", CodecTests[Tuple1[Int]].codec)
  checkAll("Codec[(Int, Int)]", CodecTests[(Int, Int)].codec)
  checkAll("Codec[(Int, Int, Foo)]", CodecTests[(Int, Int, Foo)].codec)
  checkAll("Codec[Qux[Int]]", CodecTests[Qux[Int]].codec)
  checkAll("Codec[Foo]", CodecTests[Foo].codec)

  test("Decoder[Int => Qux[String]]") {
    check {
      forAll { (i: Int, s: String) =>
        Json.obj("a" -> Json.string(s)).as[Int => Qux[String]].map(_(i)) === Xor.right(Qux(i, s))
      }
    }
  }

  test("Decoder[Qux[String] => Qux[String]]") {
    check {
      forAll { (q: Qux[String], i: Option[Int], a: Option[String]) =>
        val json = Json.obj(
          "i" -> Encoder[Option[Int]].apply(i),
          "a" -> Encoder[Option[String]].apply(a)
        )

        val expected = Qux[String](i.getOrElse(q.i), a.getOrElse(q.a))

        json.as[Qux[String] => Qux[String]].map(_(q)) === Xor.right(expected)
      }
    }
  }

  test("Tuples should be encoded as JSON arrays") {
    check {
      forAll { (t: (Int, String, Char)) =>
        val json = Encoder[(Int, String, Char)].apply(t)
        val target = Json.array(Json.int(t._1), Json.string(t._2), Encoder[Char].apply(t._3))

        json === target && json.as[(Int, String, Char)] === Xor.right(t)
      }
    }
  }

  test("Generic instances should not interfere with base instances") {
    check {
      forAll { (is: List[Int]) =>
        val json = Encoder[List[Int]].apply(is)

        json === Json.fromValues(is.map(Json.int)) && json.as[List[Int]] === Xor.right(is)
      }
    }
  }

  test("Decoding a JSON array without enough elements into a tuple should fail") {
    check {
      forAll { (i: Int, s: String) =>
        Json.array(Json.int(i), Json.string(s)).as[(Int, String, Double)].isLeft
      }
    }
  }

  test("Decoding a JSON array with too many elements into a tuple should fail") {
    check {
      forAll { (i: Int, s: String, d: Double) =>
        Json.array(Json.int(i), Json.string(s), Json.numberOrNull(d)).as[(Int, String)].isLeft
      }
    }
  }

  test("Decoding with Decoder[CNil] should fail") {
    assert(Json.empty.as[CNil].isLeft)
  }

  test("Encoding with Encoder[CNil] should throw an exception") {
    intercept[RuntimeException](Encoder[CNil].apply(null: CNil))
  }
}
