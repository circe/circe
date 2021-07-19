package io.circe.generic.simple

import cats.kernel.Eq
import io.circe.{ Codec, Decoder, Encoder, Json }
import io.circe.generic.simple.decoding.DerivedDecoder
import io.circe.generic.simple.encoding.DerivedAsObjectEncoder
import io.circe.generic.simple.semiauto._
import io.circe.testing.CodecTests
import io.circe.tests.CirceMunitSuite
import io.circe.tests.examples._
import org.scalacheck.{ Arbitrary, Gen }
import org.scalacheck.Prop.forAll
import shapeless.Witness, shapeless.labelled.{ FieldType, field }
import shapeless.test.illTyped

object SemiautoDerivedSuite {
  implicit def decodeBox[A: Decoder]: Decoder[Box[A]] = deriveDecoder
  implicit def encodeBox[A: Encoder]: Encoder[Box[A]] = deriveEncoder
  def codecForBox[A: Decoder: Encoder]: Codec[Box[A]] = deriveCodec

  implicit def decodeQux[A: Decoder]: Decoder[Qux[A]] = deriveDecoder
  implicit def encodeQux[A: Encoder]: Encoder[Qux[A]] = deriveEncoder
  def codecForQux[A: Decoder: Encoder]: Codec[Qux[A]] = deriveCodec

  implicit val decodeWub: Decoder[Wub] = deriveDecoder
  implicit val encodeWub: Encoder.AsObject[Wub] = deriveEncoder
  val codecForWub: Codec.AsObject[Wub] = deriveCodec

  implicit val codecForBar: Codec.AsObject[Bar] = deriveCodec
  implicit val codecForBaz: Codec.AsObject[Baz] = deriveCodec
  implicit val codecForBam: Codec.AsObject[Bam] = deriveCodec

  implicit val decodeFoo: Decoder[Foo] = deriveDecoder
  implicit val encodeFoo: Encoder.AsObject[Foo] = deriveEncoder
  val codecForFoo: Codec.AsObject[Foo] = deriveCodec

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
      )
    else Arbitrary.arbitrary[String].map(BaseAdtExample(_))

    implicit val arbitraryRecursiveAdtExample: Arbitrary[RecursiveAdtExample] =
      Arbitrary(atDepth(0))

    implicit val codecForBaseAdtExample: Codec.AsObject[BaseAdtExample] = deriveCodec
    implicit val codecForNestedAdtExample: Codec.AsObject[NestedAdtExample] = deriveCodec

    implicit val decodeRecursiveAdtExample: Decoder[RecursiveAdtExample] = deriveDecoder
    implicit val encodeRecursiveAdtExample: Encoder.AsObject[RecursiveAdtExample] = deriveEncoder
    val codecForRecursiveAdtExample: Codec.AsObject[RecursiveAdtExample] = deriveCodec
  }

  case class RecursiveWithOptionExample(o: Option[RecursiveWithOptionExample])

  object RecursiveWithOptionExample {
    implicit val eqRecursiveWithOptionExample: Eq[RecursiveWithOptionExample] =
      Eq.fromUniversalEquals

    private def atDepth(depth: Int): Gen[RecursiveWithOptionExample] = if (depth < 3)
      Gen.oneOf(
        Gen.const(RecursiveWithOptionExample(None)),
        atDepth(depth + 1)
      )
    else Gen.const(RecursiveWithOptionExample(None))

    implicit val arbitraryRecursiveWithOptionExample: Arbitrary[RecursiveWithOptionExample] =
      Arbitrary(atDepth(0))

    implicit val decodeRecursiveWithOptionExample: Decoder[RecursiveWithOptionExample] =
      deriveDecoder

    implicit val encodeRecursiveWithOptionExample: Encoder.AsObject[RecursiveWithOptionExample] =
      deriveEncoder

    val codecForRecursiveWithOptionExample: Codec.AsObject[RecursiveWithOptionExample] =
      deriveCodec
  }

  case class OvergenerationExampleInner(i: Int)
  case class OvergenerationExampleOuter0(i: OvergenerationExampleInner)
  case class OvergenerationExampleOuter1(oi: Option[OvergenerationExampleInner])
}

class SemiautoDerivedSuite extends CirceMunitSuite {
  import SemiautoDerivedSuite._

  checkAll("Codec[Tuple1[Int]]", CodecTests[Tuple1[Int]].codec)
  checkAll("Codec[(Int, Int, Foo)]", CodecTests[(Int, Int, Foo)].codec)
  checkAll("Codec[Box[Int]]", CodecTests[Box[Int]].codec)
  checkAll("Codec[Box[Int]] via Codec", CodecTests[Box[Int]](codecForBox[Int], codecForBox[Int]).codec)
  checkAll("Codec[Box[Int]] via Decoder and Codec", CodecTests[Box[Int]](implicitly, codecForBox[Int]).codec)
  checkAll("Codec[Box[Int]] via Encoder and Codec", CodecTests[Box[Int]](codecForBox[Int], implicitly).codec)
  checkAll("Codec[Qux[Int]]", CodecTests[Qux[Int]].codec)
  checkAll("Codec[Qux[Int]] via Codec", CodecTests[Qux[Int]](codecForQux[Int], codecForQux[Int]).codec)
  checkAll("Codec[Qux[Int]] via Decoder and Codec", CodecTests[Qux[Int]](implicitly, codecForQux[Int]).codec)
  checkAll("Codec[Qux[Int]] via Encoder and Codec", CodecTests[Qux[Int]](codecForQux[Int], implicitly).codec)
  checkAll("Codec[Seq[Foo]]", CodecTests[Seq[Foo]].codec)
  checkAll("Codec[Baz]", CodecTests[Baz].codec)
  checkAll("Codec[Foo]", CodecTests[Foo].codec)
  checkAll("Codec[Foo] via Codec", CodecTests[Foo](codecForFoo, codecForFoo).codec)
  checkAll("Codec[Foo] via Decoder and Codec", CodecTests[Foo](implicitly, codecForFoo).codec)
  checkAll("Codec[Foo] via Encoder and Codec", CodecTests[Foo](codecForFoo, implicitly).codec)
  checkAll("Codec[RecursiveAdtExample]", CodecTests[RecursiveAdtExample].codec)
  checkAll(
    "Codec[RecursiveAdtExample] via Codec",
    CodecTests[RecursiveAdtExample](
      RecursiveAdtExample.codecForRecursiveAdtExample,
      RecursiveAdtExample.codecForRecursiveAdtExample
    ).codec
  )
  checkAll(
    "Codec[RecursiveAdtExample] via Decoder and Codec",
    CodecTests[RecursiveAdtExample](implicitly, RecursiveAdtExample.codecForRecursiveAdtExample).codec
  )
  checkAll(
    "Codec[RecursiveAdtExample] via Encoder and Codec",
    CodecTests[RecursiveAdtExample](RecursiveAdtExample.codecForRecursiveAdtExample, implicitly).codec
  )
  checkAll("Codec[RecursiveWithOptionExample]", CodecTests[RecursiveWithOptionExample].codec)
  checkAll(
    "Codec[RecursiveWithOptionExample] via Codec",
    CodecTests[RecursiveWithOptionExample](
      RecursiveWithOptionExample.codecForRecursiveWithOptionExample,
      RecursiveWithOptionExample.codecForRecursiveWithOptionExample
    ).codec
  )
  checkAll(
    "Codec[RecursiveWithOptionExample] via Decoder and Codec",
    CodecTests[RecursiveWithOptionExample](implicitly, RecursiveWithOptionExample.codecForRecursiveWithOptionExample).codec
  )
  checkAll(
    "Codec[RecursiveWithOptionExample] via Encoder and Codec",
    CodecTests[RecursiveWithOptionExample](RecursiveWithOptionExample.codecForRecursiveWithOptionExample, implicitly).codec
  )

  property("Decoder[Int => Qux[String]] should decode partial JSON representations"){ forAll { (i: Int, s: String, j: Int) =>
    val json = Json.obj("a" -> Json.fromString(s), "j" -> Json.fromInt(j))
    val result = json.as[Int => Qux[String]].map(_(i))
    assertEquals(result, Right(Qux(i, s, j)))
  }}

  test("Decoder[Int => Qux[String]] should return as many errors as invalid elements in a partial case class") {
    val decoded = deriveFor[Int => Qux[String]].incomplete.decodeAccumulating(Json.obj().hcursor)

    assertEquals(decoded.fold(_.tail.size + 1, _ => 0), 2)
  }

  property("Decoder[FieldType[Witness.`'j`.T, Int] => Qux[String]] should decode partial JSON representations") {
    forAll { (i: Int, s: String, j: Int) =>
      val json = Json.obj("i" -> Json.fromInt(i), "a" -> Json.fromString(s))
      val result = json.as[FieldType[Witness.`'j`.T, Int] => Qux[String]].map(_(field(j)))

      assertEquals(result, Right(Qux(i, s, j)))
    }
  }

  property("Decoder[Qux[String] => Qux[String]] should decode patch JSON representations") {
    forAll { (q: Qux[String], i: Option[Int], a: Option[String], j: Option[Int]) =>
      val json = Json.obj(
        "i" -> Encoder[Option[Int]].apply(i),
        "a" -> Encoder[Option[String]].apply(a),
        "j" -> Encoder[Option[Int]].apply(j)
      )

      val expected = Qux[String](i.getOrElse(q.i), a.getOrElse(q.a), j.getOrElse(q.j))

      assertEquals(json.as[Qux[String] => Qux[String]].map(_(q)), Right(expected))
    }
  }

  group("A generically derived codec should"){
    property("not interfere with base instances"){ forAll { (is: List[Int]) =>
      val json = Encoder[List[Int]].apply(is)

      assertEquals(json, Json.fromValues(is.map(Json.fromInt)))
      assertEquals(json.as[List[Int]], Right(is))
    }}

    test("not come from nowhere") {
      implicitly[DerivedDecoder[OvergenerationExampleInner]]
      illTyped("Decoder[OvergenerationExampleInner]")

      implicitly[DerivedAsObjectEncoder[OvergenerationExampleInner]]
      illTyped("Encoder.AsObject[OvergenerationExampleInner]")

      illTyped("Decoder[OvergenerationExampleOuter0]")
      illTyped("Encoder.AsObject[OvergenerationExampleOuter0]")
      illTyped("Decoder[OvergenerationExampleOuter1]")
      illTyped("Encoder.AsObject[OvergenerationExampleOuter1]")
    }

    test("require instances for all parts") {
      illTyped("deriveDecoder[OvergenerationExampleInner0]")
      illTyped("deriveDecoder[OvergenerationExampleInner1]")
      illTyped("deriveEncoder[OvergenerationExampleInner0]")
      illTyped("deriveEncoder[OvergenerationExampleInner1]")
    }

  }
}
