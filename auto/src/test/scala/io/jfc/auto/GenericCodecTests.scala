package io.jfc.auto

import algebra.Eq
import org.scalacheck.{ Arbitrary, Gen }
import cats.data.{ NonEmptyList, Validated, Xor }
import cats.laws.discipline.eq._
import io.jfc.{ Decoder, Encoder, Json }
import io.jfc.test.{ CodecTests, JfcSuite }
import org.scalacheck.Prop.forAll
import shapeless._

class GenericCodecTests extends JfcSuite {
  case class Qux[A](i: Int, a: A)

  object Qux {
    implicit def eqQux[A: Eq]: Eq[Qux[A]] = Eq.by(_.a)

    implicit def arbitraryQux[A](implicit A: Arbitrary[A]): Arbitrary[Qux[A]] =
      Arbitrary(
        for {
          i <- Arbitrary.arbitrary[Int]
          a <- A.arbitrary
        } yield Qux(i, a)
      )
  }

  case class Wub(x: Long)

  object Wub {
    implicit val eqWub: Eq[Wub] = Eq.by(_.x)

    implicit val arbitraryWub: Arbitrary[Wub] =
      Arbitrary(Arbitrary.arbitrary[Long].map(Wub(_)))
  }

  sealed trait Foo
  case class Bar(i: Int, s: String) extends Foo
  case class Baz(xs: List[String]) extends Foo
  case class Bam(w: Wub, d: Double) extends Foo

  object Foo {
    implicit val eqFoo: Eq[Foo] = Eq.fromUniversalEquals

    implicit val arbitraryFoo: Arbitrary[Foo] = Arbitrary(
      Gen.oneOf(
        for {
          i <- Arbitrary.arbitrary[Int]
          s <- Arbitrary.arbitrary[String]
        } yield Bar(i, s),
        Gen.listOf(Arbitrary.arbitrary[String]).map(Baz.apply),
        for {
          w <- Arbitrary.arbitrary[Wub]
          d <- Arbitrary.arbitrary[Double]
        } yield Bam(w, d)
      )
    )
  }

  //checkAll("Codec[Tuple1[Int]]", CodecTests[Tuple1[Int]].codec)
  //checkAll("Codec[(Int, Int)]", CodecTests[(Int, Int)].codec)
  //checkAll("Codec[(Int, Int, Int)]", CodecTests[(Int, Int, Int)].codec)
  checkAll("Codec[Qux[Int]]", CodecTests[Qux[Int]].codec)
  checkAll("Codec[Foo]", CodecTests[Foo].codec)

  /*test("Decoder[Int => Qux[String]]") {
    check {
      forAll { (i: Int, s: String) =>
        Json.obj("a" -> Json.string(s)).as[Int => Qux[String]].map(_(i)) === Xor.right(Qux(i, s))
      }
    }
  }*/

  /*test("Decoding a JSON array without enough elements into a tuple should fail") {
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
  }*/
}
