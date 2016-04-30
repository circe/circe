package io.circe.generic

import algebra.Eq
import cats.data.Xor
import io.circe.{ Decoder, Encoder, Json, ObjectEncoder }
import io.circe.generic.decoding.DerivedDecoder
import io.circe.generic.encoding.DerivedObjectEncoder
import io.circe.generic.semiauto._
import io.circe.tests.{ CodecTests, CirceSuite }
import io.circe.tests.examples._
import org.scalacheck.{ Arbitrary, Gen }
import shapeless.Witness, shapeless.labelled.{ FieldType, field }
import shapeless.test.illTyped

class SemiautoDerivedSuite extends CirceSuite {
  implicit def decodeBox[A: Decoder]: Decoder[Box[A]] = deriveDecoder
  implicit def encodeBox[A: Encoder]: Encoder[Box[A]] = deriveEncoder

  implicit def decodeQux[A: Decoder]: Decoder[Qux[A]] = deriveDecoder
  implicit def encodeQux[A: Encoder]: Encoder[Qux[A]] = deriveEncoder

  implicit val decodeWub: Decoder[Wub] = deriveDecoder
  implicit val encodeWub: ObjectEncoder[Wub] = deriveEncoder
  implicit val decodeFoo: Decoder[Foo] = deriveDecoder
  implicit val encodeFoo: ObjectEncoder[Foo] = deriveEncoder

  implicit val decodeIntlessQux: Decoder[Int => Qux[String]] =
    deriveFor[Int => Qux[String]].incomplete

  implicit val decodeJlessQux: Decoder[FieldType[Witness.`'j`.T, Int] => Qux[String]] =
    deriveFor[FieldType[Witness.`'j`.T, Int] => Qux[String]].incomplete

  implicit val decodeQuxPatch: Decoder[Qux[String] => Qux[String]] = deriveFor[Qux[String]].patch

  sealed trait RecursiveAdtExample
  case class BaseAdtExample(a: String) extends RecursiveAdtExample
  case class NestedAdtExample(r: RecursiveAdtExample) extends RecursiveAdtExample

  object RecursiveAdtExample {
    implicit val eqRecursiveAdtExample: Eq[RecursiveAdtExample] = Eq.fromUniversalEquals

    private def atDepth(depth: Int): Gen[RecursiveAdtExample] = if (depth < 3)
      Gen.oneOf(
        Arbitrary.arbitrary[String].map(BaseAdtExample(_)),
        atDepth(depth + 1).map(NestedAdtExample(_))
      ) else Arbitrary.arbitrary[String].map(BaseAdtExample(_))

    implicit val arbitraryRecursiveAdtExample: Arbitrary[RecursiveAdtExample] =
      Arbitrary(atDepth(0))

    implicit val decodeRecursiveAdtExample: Decoder[RecursiveAdtExample] = deriveDecoder
    implicit val encodeRecursiveAdtExample: ObjectEncoder[RecursiveAdtExample] = deriveEncoder
  }

  case class RecursiveWithOptionExample(o: Option[RecursiveWithOptionExample])

  object RecursiveWithOptionExample {
    implicit val eqRecursiveWithOptionExample: Eq[RecursiveWithOptionExample] =
      Eq.fromUniversalEquals

    private def atDepth(depth: Int): Gen[RecursiveWithOptionExample] = if (depth < 3)
      Arbitrary.arbitrary[Option[RecursiveWithOptionExample]].map(
        RecursiveWithOptionExample(_)
      ) else Gen.const(RecursiveWithOptionExample(None))

    implicit val arbitraryRecursiveWithOptionExample: Arbitrary[RecursiveWithOptionExample] =
      Arbitrary(atDepth(0))

    implicit val decodeRecursiveWithOptionExample: Decoder[RecursiveWithOptionExample] =
      deriveDecoder

    implicit val encodeRecursiveWithOptionExample: ObjectEncoder[RecursiveWithOptionExample] =
      deriveEncoder
  }

  case class OvergenerationExampleInner(i: Int)
  case class OvergenerationExampleOuter0(i: OvergenerationExampleInner)
  case class OvergenerationExampleOuter1(oi: Option[OvergenerationExampleInner])

  checkLaws("Codec[Tuple1[Int]]", CodecTests[Tuple1[Int]].codec)
  checkLaws("Codec[(Int, Int, Foo)]", CodecTests[(Int, Int, Foo)].codec)
  checkLaws("Codec[Box[Int]]", CodecTests[Box[Int]].codec)
  checkLaws("Codec[Qux[Int]]", CodecTests[Qux[Int]].codec)
  checkLaws("Codec[Foo]", CodecTests[Foo].codec)
  checkLaws("Codec[RecursiveAdtExample]", CodecTests[RecursiveAdtExample].codec)
  checkLaws("Codec[RecursiveWithOptionExample]", CodecTests[RecursiveWithOptionExample].codec)

  "Decoder[Int => Qux[String]]" should "decode partial JSON representations" in forAll { (i: Int, s: String, j: Int) =>
    val result = Json.obj(
      "a" -> Json.fromString(s),
      "j" -> Json.fromInt(j)
    ).as[Int => Qux[String]].map(_(i))

    assert(result === Xor.right(Qux(i, s, j)))
  }

  "Decoder[FieldType[Witness.`'j`.T, Int] => Qux[String]]" should "decode partial JSON representations" in {
    forAll { (i: Int, s: String, j: Int) =>
      val result = Json.obj(
        "i" -> Json.fromInt(i),
        "a" -> Json.fromString(s)
      ).as[FieldType[Witness.`'j`.T, Int] => Qux[String]].map(
         _(field(j))
      )

      assert(result === Xor.right(Qux(i, s, j)))
    }
  }

  "Decoder[Qux[String] => Qux[String]]" should "decode patch JSON representations" in {
    forAll { (q: Qux[String], i: Option[Int], a: Option[String], j: Option[Int]) =>
      val json = Json.obj(
        "i" -> Encoder[Option[Int]].apply(i),
        "a" -> Encoder[Option[String]].apply(a),
        "j" -> Encoder[Option[Int]].apply(j)
      )

      val expected = Qux[String](i.getOrElse(q.i), a.getOrElse(q.a), j.getOrElse(q.j))

      assert(json.as[Qux[String] => Qux[String]].map(_(q)) === Xor.right(expected))
    }
  }

  "A generically derived codec" should "not interfere with base instances" in forAll { (is: List[Int]) =>
    val json = Encoder[List[Int]].apply(is)

    assert(json === Json.fromValues(is.map(Json.fromInt)) && json.as[List[Int]] === Xor.right(is))
  }

  it should "not come from nowhere" in {
    implicitly[DerivedDecoder[OvergenerationExampleInner]]
    illTyped("Decoder[OvergenerationExampleInner]")

    implicitly[DerivedObjectEncoder[OvergenerationExampleInner]]
    illTyped("ObjectEncoder[OvergenerationExampleInner]")

    illTyped("Decoder[OvergenerationExampleOuter0]")
    illTyped("ObjectEncoder[OvergenerationExampleOuter0]")
    illTyped("Decoder[OvergenerationExampleOuter1]")
    illTyped("ObjectEncoder[OvergenerationExampleOuter1]")
  }

  it should "require instances for all parts" in {
    illTyped("deriveDecoder[OvergenerationExampleInner0]")
    illTyped("deriveDecoder[OvergenerationExampleInner1]")
    illTyped("deriveEncoder[OvergenerationExampleInner0]")
    illTyped("deriveEncoder[OvergenerationExampleInner1]")
  }
}
