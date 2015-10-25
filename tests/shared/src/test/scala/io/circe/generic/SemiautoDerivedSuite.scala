package io.circe.generic

import cats.data.Xor
import io.circe.{ Decoder, Encoder, Json }
import io.circe.generic.semiauto._
import io.circe.tests.{ CodecTests, CirceSuite }
import io.circe.tests.examples._
import org.scalacheck.Prop.forAll
import shapeless.{ CNil, Witness }, shapeless.labelled.{ FieldType, field }

class SemiautoDerivedSuite extends CirceSuite {
  implicit def decodeQux[A: Decoder]: Decoder[Qux[A]] = deriveFor[Qux[A]].decoder
  implicit def encodeQux[A: Encoder]: Encoder[Qux[A]] = deriveFor[Qux[A]].encoder
  implicit val decodeWub: Decoder[Wub] = deriveFor[Wub].decoder
  implicit val encodeWub: Encoder[Wub] = deriveFor[Wub].encoder
  implicit val decodeFoo: Decoder[Foo] = ??? //deriveFor[Foo].decoder
  implicit val encodeFoo: Encoder[Foo] = deriveFor[Foo].encoder

  implicit val decodeIntlessQux: Decoder[Int => Qux[String]] =
    deriveFor[Int => Qux[String]].incomplete

  implicit val decodeJlessQux: Decoder[FieldType[Witness.`'j`.T, Int] => Qux[String]] =
    deriveFor[FieldType[Witness.`'j`.T, Int] => Qux[String]].incomplete

  implicit val decodeQuxPatch: Decoder[Qux[String] => Qux[String]] = deriveFor[Qux[String]].patch

  checkAll("Codec[Tuple1[Int]]", CodecTests[Tuple1[Int]].codec)
  checkAll("Codec[(Int, Int, Foo)]", CodecTests[(Int, Int, Foo)].codec)
  checkAll("Codec[Qux[Int]]", CodecTests[Qux[Int]].codec)
  checkAll("Codec[Foo]", CodecTests[Foo].codec)

  test("Decoder[Int => Qux[String]]") {
    check {
      forAll { (i: Int, s: String, j: Int) =>
        Json.obj(
          "a" -> Json.string(s),
          "j" -> Json.int(j)
        ).as[Int => Qux[String]].map(_(i)) === Xor.right(Qux(i, s, j))
      }
    }
  }

  test("Decoder[FieldType[Witness.`'j`.T, Int] => Qux[String]]") {
    check {
      forAll { (i: Int, s: String, j: Int) =>
        Json.obj(
          "i" -> Json.int(i),
          "a" -> Json.string(s)
        ).as[FieldType[Witness.`'j`.T, Int] => Qux[String]].map(
          _(field(j))
        ) === Xor.right(Qux(i, s, j))
      }
    }
  }

  test("Decoder[Qux[String] => Qux[String]]") {
    check {
      forAll { (q: Qux[String], i: Option[Int], a: Option[String], j: Option[Int]) =>
        val json = Json.obj(
          "i" -> Encoder[Option[Int]].apply(i),
          "a" -> Encoder[Option[String]].apply(a),
          "j" -> Encoder[Option[Int]].apply(j)
        )

        val expected = Qux[String](i.getOrElse(q.i), a.getOrElse(q.a), j.getOrElse(q.j))

        json.as[Qux[String] => Qux[String]].map(_(q)) === Xor.right(expected)
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
}
