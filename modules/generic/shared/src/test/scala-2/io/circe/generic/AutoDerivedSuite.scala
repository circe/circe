package io.circe.generic

import cats.kernel.Eq
import cats.syntax.contravariant._
import io.circe.{ Decoder, Encoder, Json }
import io.circe.generic.auto._
import io.circe.testing.CodecTests
import io.circe.tests.CirceMunitSuite
import io.circe.tests.examples._
import org.scalacheck.{ Arbitrary, Gen }
import org.scalacheck.Prop.forAll
import shapeless.Witness, shapeless.labelled.{ FieldType, field }
import shapeless.test.illTyped

object AutoDerivedSuite {
  case class InnerCaseClassExample(a: String, b: String, c: String, d: String)
  case class OuterCaseClassExample(a: String, inner: InnerCaseClassExample)

  object InnerCaseClassExample {
    implicit val arbitraryInnerCaseClassExample: Arbitrary[InnerCaseClassExample] =
      Arbitrary(
        for {
          a <- Arbitrary.arbitrary[String]
          b <- Arbitrary.arbitrary[String]
          c <- Arbitrary.arbitrary[String]
          d <- Arbitrary.arbitrary[String]
        } yield InnerCaseClassExample(a, b, c, d)
      )
  }

  object OuterCaseClassExample {
    implicit val eqOuterCaseClassExample: Eq[OuterCaseClassExample] = Eq.fromUniversalEquals

    implicit val arbitraryOuterCaseClassExample: Arbitrary[OuterCaseClassExample] =
      Arbitrary(
        for {
          a <- Arbitrary.arbitrary[String]
          i <- Arbitrary.arbitrary[InnerCaseClassExample]
        } yield OuterCaseClassExample(a, i)
      )
  }

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
  }

  case class RecursiveWithOptionExample(o: Option[RecursiveWithOptionExample])

  object RecursiveWithOptionExample {
    implicit val eqRecursiveWithOptionExample: Eq[RecursiveWithOptionExample] =
      Eq.fromUniversalEquals

    private def atDepth(depth: Int): Gen[RecursiveWithOptionExample] = if (depth < 3)
      Gen
        .option(atDepth(depth + 1))
        .map(
          RecursiveWithOptionExample(_)
        )
    else Gen.const(RecursiveWithOptionExample(None))

    implicit val arbitraryRecursiveWithOptionExample: Arbitrary[RecursiveWithOptionExample] =
      Arbitrary(atDepth(0))
  }

  import shapeless.tag
  import shapeless.tag.@@

  trait Tag1
  trait Tag2
  case class WithTaggedMembers(i: List[Int] @@ Tag1, s: String @@ Tag2)

  implicit val encodeIntTag1: Encoder[List[Int] @@ Tag1] = Encoder[List[Int]].narrow
  implicit val encodeStringTag2: Encoder[String @@ Tag2] = Encoder[String].narrow
  implicit val decodeIntTag1: Decoder[List[Int] @@ Tag1] = Decoder[List[Int]].map(tag[Tag1](_))
  implicit val decodeStringTag2: Decoder[String @@ Tag2] = Decoder[String].map(tag[Tag2](_))

  object WithTaggedMembers {
    implicit val eqWithTaggedMembers: Eq[WithTaggedMembers] = Eq.fromUniversalEquals

    implicit val arbitraryWithTaggedMembers: Arbitrary[WithTaggedMembers] = Arbitrary(
      for {
        i <- Arbitrary.arbitrary[List[Int]]
        s <- Arbitrary.arbitrary[String]
      } yield WithTaggedMembers(tag[Tag1](i), tag[Tag2](s))
    )
  }

  trait Tag
  case class WithSeqOfTagged(s: Vector[String @@ Tag])

  implicit val encodeStringTag: Encoder[String @@ Tag] = Encoder[String].narrow
  implicit val decodeStringTag: Decoder[String @@ Tag] = Decoder[String].map(tag[Tag](_))

  object WithSeqOfTagged {
    implicit val eqSeqOfWithSeqOfTagged: Eq[Seq[WithSeqOfTagged]] = Eq.fromUniversalEquals

    implicit val arbitraryWithSeqOfTagged: Arbitrary[WithSeqOfTagged] = Arbitrary(
      for {
        s <- Arbitrary.arbitrary[Vector[String]]
      } yield WithSeqOfTagged(s.map(tag[Tag](_)))
    )
  }
}

class AutoDerivedSuite extends CirceMunitSuite {
  import AutoDerivedSuite._

  checkAll("Codec[Tuple1[Int]]", CodecTests[Tuple1[Int]].codec)
  checkAll("Codec[(Int, Int, Foo)]", CodecTests[(Int, Int, Foo)].codec)
  checkAll("Codec[Qux[Int]]", CodecTests[Qux[Int]].codec)
  checkAll("Codec[Seq[Foo]]", CodecTests[Seq[Foo]].codec)
  checkAll("Codec[Baz]", CodecTests[Baz].codec)
  checkAll("Codec[Foo]", CodecTests[Foo].codec)
  checkAll("Codec[OuterCaseClassExample]", CodecTests[OuterCaseClassExample].codec)
  checkAll("Codec[RecursiveAdtExample]", CodecTests[RecursiveAdtExample].codec)
  checkAll("Codec[RecursiveWithOptionExample]", CodecTests[RecursiveWithOptionExample].codec)

  property("Decoder[Int => Qux[String]] should decode partial JSON representations") {
    forAll { (i: Int, s: String, j: Int) =>
      val json = Json.obj("a" -> Json.fromString(s), "j" -> Json.fromInt(j))
      val result = json.as[Int => Qux[String]].map(_(i))
      assertEquals(result, Right(Qux(i, s, j)))
    }
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

  group("A generically derived codec should") {
    property(" not interfere with base instances") {
      forAll { (is: List[Int]) =>
        val json = Encoder[List[Int]].apply(is)
        assertEquals(json, Json.fromValues(is.map(Json.fromInt)))
        assertEquals(json.as[List[Int]], Right(is))
      }
    }

    test("not be derived for Object") {
      illTyped("Decoder[Object]")
      illTyped("Encoder[Object]")
    }

    test("not be derived for AnyRef") {
      illTyped("Decoder[AnyRef]")
      illTyped("Encoder[AnyRef]")
    }

  }

  property("Generic decoders should not interfere with defined decoders") {
    forAll { (xs: List[String]) =>
      val json = Json.obj("Baz" -> Json.fromValues(xs.map(Json.fromString)))
      assertEquals(Decoder[Foo].apply(json.hcursor), Right(Baz(xs): Foo))
    }
  }

  property("Generic encoders should not interfere with defined encoders") {
    forAll { (xs: List[String]) =>
      val json = Json.obj("Baz" -> Json.fromValues(xs.map(Json.fromString)))

      assertEquals(Encoder[Foo].apply(Baz(xs): Foo), json)
    }
  }

  checkAll("Codec[WithTaggedMembers]", CodecTests[WithTaggedMembers].codec)
  checkAll("Codec[Seq[WithSeqOfTagged]]", CodecTests[Seq[WithSeqOfTagged]].codec)
}
