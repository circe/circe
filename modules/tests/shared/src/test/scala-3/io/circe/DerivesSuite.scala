/*
 * Copyright 2024 circe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.circe

import cats.kernel.Eq
import cats.kernel.instances.all._
import cats.syntax.eq._
import io.circe.{ Codec, Decoder, Encoder, Json }
import io.circe.testing.CodecTests
import io.circe.tests.CirceMunitSuite
import org.scalacheck.{ Arbitrary, Gen }

object DerivesSuite {
  case class Box[A](a: A) derives Decoder, Encoder

  object Box {
    implicit def eqBox[A: Eq]: Eq[Box[A]] = Eq.by(_.a)
    implicit def arbitraryBox[A](implicit A: Arbitrary[A]): Arbitrary[Box[A]] = Arbitrary(A.arbitrary.map(Box(_)))
  }

  case class InnerBox[A](inner: A) derives Decoder, Encoder

  object InnerBox {
    given eqInnerBox[A: Eq]: Eq[InnerBox[A]] = Eq.by(_.inner)
    given arbitraryInnerBox[A](using A: Arbitrary[A]): Arbitrary[InnerBox[A]] = Arbitrary(
      A.arbitrary.map(InnerBox(_))
    )
  }

  case class WithNullables(
                            a: String,
                            b: Nullable[String],
                            c: Nullable[Int],
                            d: Nullable[Boolean],
                            e: Nullable[Box[String]],
                            f: Nullable[List[String]],
                            g: Option[Box[String]]
                          ) derives Decoder,
  Encoder.AsObject

  object WithNullables {
    implicit def eqNullable[A]: Eq[Nullable[A]] = Eq.fromUniversalEquals
    given Eq[WithNullables] = Eq.fromUniversalEquals
    given Arbitrary[WithNullables] = {
      given aNullable[A](using A: Arbitrary[A]): Arbitrary[Nullable[A]] =
      Arbitrary[Nullable[A]](
        A.arbitrary.flatMap { a =>
          summon[Arbitrary[Int]].arbitrary.map {
            case byte if byte % 3 == 0 =>
              Nullable.Null: Nullable[A]
            case byte if byte % 3 == 1 =>
              Nullable.Undefined: Nullable[A]
            case _ =>
              Nullable.Value(a): Nullable[A]
          }
        }
      )

      val gen = for {
        a <- summon[Arbitrary[String]].arbitrary
        b <- summon[Arbitrary[Nullable[String]]].arbitrary
        c <- summon[Arbitrary[Nullable[Int]]].arbitrary
        d <- summon[Arbitrary[Nullable[Boolean]]].arbitrary
        e <- summon[Arbitrary[Nullable[Box[String]]]].arbitrary
        f <- summon[Arbitrary[Nullable[List[String]]]].arbitrary
        g <- summon[Arbitrary[Option[Box[String]]]].arbitrary
      } yield WithNullables(a, b, c, d, e, f, g)
      Arbitrary(gen)
    }

  }

  case class Qux[A](i: Int, a: A, j: Int) derives Codec

  object Qux {
    given codec[A: Encoder: Decoder]: Codec[Qux[A]] = Codec.AsObject.derived
    given eqQux[A: Eq]: Eq[Qux[A]] = Eq.by(q => (q.i, q.a, q.j))

    given arbitraryQux[A](using A: Arbitrary[A]): Arbitrary[Qux[A]] =
      Arbitrary(
        for {
          i <- Arbitrary.arbitrary[Int]
          a <- A.arbitrary
          j <- Arbitrary.arbitrary[Int]
        } yield Qux(i, a, j)
      )
  }

  case class Wub(x: Long) derives Codec.AsObject

  object Wub {
    given Eq[Wub] = Eq.by(_.x)
    given Arbitrary[Wub] = Arbitrary(Arbitrary.arbitrary[Long].map(Wub(_)))
  }

  sealed trait Foo derives Codec.AsObject
  case class Bar(i: Int, s: String) extends Foo
  case class Baz(xs: List[String]) extends Foo
  case class Bam(w: Wub, d: Double) extends Foo derives Codec.AsObject

  object Bar {
    given Eq[Bar] = Eq.fromUniversalEquals
    given Arbitrary[Bar] = Arbitrary(
      for {
        i <- Arbitrary.arbitrary[Int]
        s <- Arbitrary.arbitrary[String]
      } yield Bar(i, s)
    )

    given Decoder[Bar] = Decoder.forProduct2("i", "s")(Bar.apply)
    given Encoder[Bar] = Encoder.forProduct2("i", "s") {
      case Bar(i, s) => (i, s)
    }
  }

  object Baz {
    given Eq[Baz] = Eq.fromUniversalEquals
    given Arbitrary[Baz] = Arbitrary(
      Arbitrary.arbitrary[List[String]].map(Baz.apply)
    )

    given Decoder[Baz] = Decoder[List[String]].map(Baz(_))
    given Encoder[Baz] = Encoder.instance {
      case Baz(xs) => Json.fromValues(xs.map(Json.fromString))
    }
  }

  object Bam {
    given Eq[Bam] = Eq.fromUniversalEquals
    given Arbitrary[Bam] = Arbitrary(
      for {
        w <- Arbitrary.arbitrary[Wub]
        d <- Arbitrary.arbitrary[Double]
      } yield Bam(w, d)
    )
  }

  object Foo {
    given Eq[Foo] = Eq.fromUniversalEquals

    given Arbitrary[Foo] = Arbitrary(
      Gen.oneOf(
        Arbitrary.arbitrary[Bar],
        Arbitrary.arbitrary[Baz],
        Arbitrary.arbitrary[Bam]
      )
    )
  }

  sealed trait RecursiveAdtExample derives Codec.AsObject
  case class BaseAdtExample(a: String) extends RecursiveAdtExample derives Codec.AsObject
  case class NestedAdtExample(r: RecursiveAdtExample) extends RecursiveAdtExample derives Codec.AsObject

  object RecursiveAdtExample {
    given Eq[RecursiveAdtExample] = Eq.fromUniversalEquals

    private def atDepth(depth: Int): Gen[RecursiveAdtExample] = if (depth < 3)
      Gen.oneOf(
        Arbitrary.arbitrary[String].map(BaseAdtExample(_)),
        atDepth(depth + 1).map(NestedAdtExample(_))
      )
    else Arbitrary.arbitrary[String].map(BaseAdtExample(_))

    given Arbitrary[RecursiveAdtExample] =
      Arbitrary(atDepth(0))
  }

  case class RecursiveWithOptionExample(o: Option[RecursiveWithOptionExample]) derives Codec.AsObject

  object RecursiveWithOptionExample {
    given Eq[RecursiveWithOptionExample] =
      Eq.fromUniversalEquals

    private def atDepth(depth: Int): Gen[RecursiveWithOptionExample] = if (depth < 3)
      Gen.oneOf(
        Gen.const(RecursiveWithOptionExample(None)),
        atDepth(depth + 1)
      )
    else Gen.const(RecursiveWithOptionExample(None))

    given Arbitrary[RecursiveWithOptionExample] =
      Arbitrary(atDepth(0))
  }

  enum Vegetable derives Codec:
    case Potato(species: String)
    case Carrot(length: Double)
    case Onion(layers: Int)
    case Turnip
  object Vegetable:
    given Eq[Vegetable] = Eq.fromUniversalEquals
    given Arbitrary[Vegetable.Potato] = Arbitrary(
      Arbitrary.arbitrary[String].map(Vegetable.Potato.apply)
    )
    given Arbitrary[Vegetable.Carrot] = Arbitrary(
      Arbitrary.arbitrary[Double].map(Vegetable.Carrot.apply)
    )
    given Arbitrary[Vegetable.Onion] = Arbitrary(
      Arbitrary.arbitrary[Int].map(Vegetable.Onion.apply)
    )
    given Arbitrary[Vegetable.Turnip.type] = Arbitrary(Gen.const(Vegetable.Turnip))
    given Arbitrary[Vegetable] = Arbitrary(
      Gen.oneOf(
        Arbitrary.arbitrary[Vegetable.Potato],
        Arbitrary.arbitrary[Vegetable.Carrot],
        Arbitrary.arbitrary[Vegetable.Onion],
        Arbitrary.arbitrary[Vegetable.Turnip.type]
      )
    )

  enum RecursiveEnumAdt derives Codec:
    case BaseAdtExample(a: String)
    case NestedAdtExample(r: RecursiveEnumAdt)
  object RecursiveEnumAdt:
    given Eq[RecursiveEnumAdt] = Eq.fromUniversalEquals

    private def atDepth(depth: Int): Gen[RecursiveEnumAdt] = if (depth < 3)
      Gen.oneOf(
        Arbitrary.arbitrary[String].map(RecursiveEnumAdt.BaseAdtExample(_)),
        atDepth(depth + 1).map(RecursiveEnumAdt.NestedAdtExample(_))
      )
    else Arbitrary.arbitrary[String].map(RecursiveEnumAdt.BaseAdtExample(_))

    given Arbitrary[RecursiveEnumAdt] = Arbitrary(atDepth(0))

  sealed trait ADTWithSubTraitExample derives Codec.AsObject
  sealed trait SubTrait extends ADTWithSubTraitExample
  case class TheClass(a: Int) extends SubTrait

  object ADTWithSubTraitExample:
    given Arbitrary[ADTWithSubTraitExample] = Arbitrary(Arbitrary.arbitrary[Int].map(TheClass.apply))
    given Eq[ADTWithSubTraitExample] = Eq.fromUniversalEquals

  case class ProductWithTaggedMember(x: ProductWithTaggedMember.TaggedString) derives Codec.AsObject

  object ProductWithTaggedMember:
    sealed trait Tag

    type TaggedString = String with Tag

    object TaggedString:
      val decoder: Decoder[TaggedString] =
        summon[Decoder[String]].map(_.asInstanceOf[TaggedString])
      val encoder: Encoder[TaggedString] =
        summon[Encoder[String]].contramap(x => x)

    given Codec[TaggedString] =
      Codec.from(TaggedString.decoder, TaggedString.encoder)

    def fromUntagged(x: String): ProductWithTaggedMember =
      ProductWithTaggedMember(x.asInstanceOf[TaggedString])

    given Arbitrary[ProductWithTaggedMember] =
      Arbitrary {
        Arbitrary.arbitrary[String].map(fromUntagged)
      }
    given Eq[ProductWithTaggedMember] = Eq.fromUniversalEquals

  case class Inner[A](field: A) derives Encoder, Decoder
  case class Outer(a: Option[Inner[String]]) derives Encoder.AsObject, Decoder
  object Outer:
    given Eq[Outer] = Eq.fromUniversalEquals
    given Arbitrary[Outer] =
      Arbitrary(Gen.option(Arbitrary.arbitrary[String].map(Inner.apply)).map(Outer.apply))
}

class DerivesSuite extends CirceMunitSuite {
  import DerivesSuite._
  import io.circe.syntax._

  checkAll("Codec[Box[Wub]]", CodecTests[Box[Wub]].codec)
  checkAll("Codec[Box[Long]]", CodecTests[Box[Long]].codec)
  // checkAll("Codec[Qux[Long]]", CodecTests[Qux[Long]].codec) Does not compile because Scala 3 requires a `Codec[Long]` for this when you use `derives Codec`
  checkAll("Codec[Seq[Foo]]", CodecTests[Seq[Foo]].codec)
  checkAll("Codec[Baz]", CodecTests[Baz].codec)
  checkAll("Codec[Foo]", CodecTests[Foo].codec)
  checkAll("Codec[RecursiveAdtExample]", CodecTests[RecursiveAdtExample].codec)
  checkAll("Codec[RecursiveWithOptionExample]", CodecTests[RecursiveWithOptionExample].codec)
  checkAll("Codec[Vegetable]", CodecTests[Vegetable].codec)
  checkAll("Codec[RecursiveEnumAdt]", CodecTests[RecursiveEnumAdt].codec)
  checkAll("Codec[ADTWithSubTraitExample]", CodecTests[ADTWithSubTraitExample].codec)
  checkAll("Codec[ProductWithTaggedMember] (#2135)", CodecTests[ProductWithTaggedMember].codec)
  checkAll("Codec[Outer]", CodecTests[Outer].codec)
  checkAll("Codec[WithNullables]", CodecTests[WithNullables].codec)

  test("Nested sums should not be encoded redundantly") {
    val foo: ADTWithSubTraitExample = TheClass(0)
    val expected = Json.obj("TheClass" -> Json.obj("a" -> 0.asJson))
    assertEquals(foo.asJson, expected)
  }

  test("Derived Encoder respects existing instances") {
    val some = Outer(Some(Inner("c")))
    val none = Outer(None)
    val expectedSome = Json.obj("a" -> Json.obj("field" -> "c".asJson))
    val expectedNone = Json.obj("a" -> Json.Null)
    assertEquals(some.asJson, expectedSome)
    assertEquals(none.asJson, expectedNone)
  }

  test("Derivation uses pre-existing given codecs") {
    import io.circe.syntax._

    {
      val foo = Box("inner value")
      val expected = Json.obj(
        "a" -> "inner value".asJson
      )
      assert(foo.asJson === expected)
    }

    {
      val foo = Box(Option("inner value"))
      val expected = Json.obj(
        "a" -> "inner value".asJson
      )
      assert(foo.asJson === expected)
    }
  }

  test("Recursive derivation works inside Option") {
    import io.circe.syntax._
    val foo = Box(Option(InnerBox("inner value")))
    val expected = Json.obj(
      "a" -> Json.obj(
        "inner" -> "inner value".asJson
      )
    )

    assert(foo.asJson === expected)
  }

  test("Nullable codecs work as expected") {
    import io.circe.syntax._

    val foo =
      WithNullables(
        a = "a value",
        b = Nullable.Value("b value"),
        c = Nullable.Undefined,
        d = Nullable.Null,
        e = Nullable.Value(Box("boxed value")),
        f = Nullable.Value(List("a", "b", "c")),
        g = Some(Box("boxed in option"))
      )

    val expected = Json.obj(
      "a" -> "a value".asJson,
      "b" -> "b value".asJson,
      "d" -> Json.Null,
      "e" -> Json.obj(
        "a" -> "boxed value".asJson
      ),
      "f" -> List("a", "b", "c").asJson,
      "g" -> Json.obj(
        "a" -> "boxed in option".asJson
      )
    )

    assert(foo.asJson === expected)
  }

}
