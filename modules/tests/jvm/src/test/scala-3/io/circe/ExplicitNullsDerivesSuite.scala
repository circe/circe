package io.circe

import java.{ math => jm }
import scala.math.{ BigDecimal, BigInt }

import cats.kernel.Eq
import io.circe.testing.CodecTests
import io.circe.tests.CirceMunitSuite
import org.scalacheck.{ Arbitrary, Gen }
import org.scalacheck.Arbitrary.{ arbBigDecimal, arbBigInt }
import scala.quoted.staging

object ExplicitNullsDerivesSuite {
  given Eq[jm.BigInteger] = Eq.fromUniversalEquals
  given Arbitrary[jm.BigInteger] = Arbitrary(
    Gen.oneOf(
      Arbitrary.arbitrary[BigInt].map(_.toString).map(jm.BigInteger(_)),
      null
    )
  )
  given Eq[jm.BigDecimal] = Eq.fromUniversalEquals
  given Arbitrary[jm.BigDecimal] = Arbitrary(
    Gen.oneOf(
      Arbitrary.arbitrary[BigDecimal].map(_.toString).map(jm.BigDecimal(_)),
      null
    )
  )

  case class Box[A](a: A) derives Decoder, Encoder.AsObject {
    given eqBox[A: Eq]: Eq[Box[A]] = Eq.by(_.a)
    given arbitraryBox[A](using A: Arbitrary[A]): Arbitrary[Box[A]] = Arbitrary(A.arbitrary.map(Box(_)))
  }

  case class Qux[A](i: jm.BigInteger, a: A, s: String) derives Codec.AsObject {
    given eqQux[A: Eq]: Eq[Qux[A]] = Eq.by(q => (q.i, q.a, q.s))
    given arbitraryQux[A](using A: Arbitrary[A]): Arbitrary[Qux[A]] = Arbitrary(
      for {
        i <- Arbitrary.arbitrary[jm.BigInteger]
        a <- A.arbitrary
        s <- Arbitrary.arbitrary[String]
      } yield Qux(i, a, s)
    )
  }

  case class Wub(x: jm.BigDecimal) derives Codec.AsObject

  object Wub {
    given Eq[Wub] = Eq.by(_.x)
    given Arbitrary[Wub] = Arbitrary(Arbitrary.arbitrary[jm.BigDecimal].map(Wub(_)))
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

    given decodeBar: Decoder[Bar] = Decoder.forProduct2("i", "s")(Bar.apply)
    given encodeBar: Encoder[Bar] = Encoder.forProduct2("i", "s") {
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

  enum Vegetable derives Codec.AsObject:
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
}

class ExplicitNullsDerivesSuite extends CirceMunitSuite {
  import ExplicitNullsDerivesSuite._

  // checkAll("Codec[Box[Wub]]", CodecTests[Box[Wub]].codec)
  // checkAll("Codec[Qux[Wub]]", CodecTests[Qux[Wub]].codec)
  checkAll("Codec[Wub]", CodecTests[Wub].codec)
  checkAll("Codec[Foo]", CodecTests[Foo].codec)
  checkAll("Codec[Seq[Foo]]", CodecTests[Seq[Foo]].codec)
  checkAll("Codec[Baz]", CodecTests[Baz].codec)
  checkAll("Codec[Vegetable]", CodecTests[Vegetable].codec)

  property("Derives under `-Yexplicit-nulls` should work") {
    val settings = staging.Compiler.Settings.make(compilerArgs = List("-Yexplicit-nulls"))
    val explicitNullsCompiler = staging.Compiler.make(getClass.getClassLoader)(settings)
    staging.run('{
      val encoder = Encoder.AsObject.derived[Box[Wub]];
      val decoder = Decoder.derived[Box[Wub]];
      val codec = Codec.AsObject.derived[Box[Wub]];
    })(using explicitNullsCompiler)
  }
}
