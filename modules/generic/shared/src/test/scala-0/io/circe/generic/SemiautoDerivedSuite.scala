package io.circe.generic

import cats.kernel.Eq
import cats.syntax.eq._
import io.circe.{ Codec, Decoder, Encoder, Json }
import io.circe.generic.semiauto._
import io.circe.testing.CodecTests
import io.circe.tests.CirceMunitSuite
import io.circe.tests.examples._
import org.scalacheck.{ Arbitrary, Gen, Prop }

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

  implicit val decodeFoo: Decoder[Foo] = deriveDecoder
  implicit val encodeFoo: Encoder.AsObject[Foo] = deriveEncoder
  val codecForFoo: Codec.AsObject[Foo] = deriveCodec

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
        atDepth(depth + 1).map(Some(_)).map(RecursiveWithOptionExample(_))
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
    CodecTests[RecursiveWithOptionExample](
      implicitly,
      RecursiveWithOptionExample.codecForRecursiveWithOptionExample
    ).codec
  )
  checkAll(
    "Codec[RecursiveWithOptionExample] via Encoder and Codec",
    CodecTests[RecursiveWithOptionExample](
      RecursiveWithOptionExample.codecForRecursiveWithOptionExample,
      implicitly
    ).codec
  )

  property("A generically derived codec should not interfere with base instances") {
    Prop.forAll { (is: List[Int]) =>
      val json = Encoder[List[Int]].apply(is)

      assert(json === Json.fromValues(is.map(Json.fromInt)) && json.as[List[Int]] === Right(is))
    }
  }

  test("A generically derived codec should not come from nowhere") {
    assertTypeError("Decoder[OvergenerationExampleInner]")

    assertTypeError("Encoder.AsObject[OvergenerationExampleInner]")

    assertTypeError("Decoder[OvergenerationExampleOuter0]")
    assertTypeError("Encoder.AsObject[OvergenerationExampleOuter0]")
    assertTypeError("Decoder[OvergenerationExampleOuter1]")
    assertTypeError("Encoder.AsObject[OvergenerationExampleOuter1]")
  }

  test("A generically derived codec should require instances for all parts") {
    assertTypeError("deriveDecoder[OvergenerationExampleInner0]")
    assertTypeError("deriveDecoder[OvergenerationExampleInner1]")
    assertTypeError("deriveEncoder[OvergenerationExampleInner0]")
    assertTypeError("deriveEncoder[OvergenerationExampleInner1]")
  }

  property("A generically derived codec for an empty case class should not accept non-objects") {
    Prop.forAll { (j: Json) =>
      case class EmptyCc()

      assert(deriveDecoder[EmptyCc].decodeJson(j).isRight == j.isObject)
      assert(deriveCodec[EmptyCc].decodeJson(j).isRight == j.isObject)
    }
  }
}
