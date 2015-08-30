package io.circe.generic

import cats.data.{ NonEmptyList, Validated, Xor }
import io.circe.{ Decoder, Encoder, Json }
import io.circe.generic.auto._
import io.circe.tests.{ CodecTests, CirceSuite }
import io.circe.tests.examples._
import org.scalacheck.Prop.forAll
import shapeless.CNil

/**
 * For some reason several of these tests fail if the test file is moved up a directory (which is
 * where it properly belongs after some recent refactoring). I'd love to know why.
 */
class AutoDerivedSuite extends CirceSuite {
  checkAll("Codec[Tuple1[Int]]", CodecTests[Tuple1[Int]].codec)
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

  test("Generic instances should not interfere with base instances") {
    check {
      forAll { (is: List[Int]) =>
        val json = Encoder[List[Int]].apply(is)

        json === Json.fromValues(is.map(Json.int)) && json.as[List[Int]] === Xor.right(is)
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
