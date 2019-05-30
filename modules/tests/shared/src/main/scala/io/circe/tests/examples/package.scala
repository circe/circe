package io.circe.tests

import cats.instances.AllInstances
import cats.kernel.Eq
import cats.syntax.functor._
import io.circe.{ Decoder, DecodingFailure, Encoder, Json }
import io.circe.testing.ArbitraryInstances
import org.scalacheck.{ Arbitrary, Gen }

package object examples extends AllInstances with ArbitraryInstances with MissingInstances {
  val glossary: Json = Json.obj(
    "glossary" -> Json.obj(
      "title" -> Json.fromString("example glossary"),
      "GlossDiv" -> Json.obj(
        "title" -> Json.fromString("S"),
        "GlossList" -> Json.obj(
          "GlossEntry" -> Json.obj(
            "ID" -> Json.fromString("SGML"),
            "SortAs" -> Json.fromString("SGML"),
            "GlossTerm" -> Json.fromString("Standard Generalized Markup Language"),
            "Acronym" -> Json.fromString("SGML"),
            "Abbrev" -> Json.fromString("ISO 8879:1986"),
            "GlossDef" -> Json.obj(
              "para" -> Json.fromString(
                "A meta-markup language, used to create markup languages such as DocBook."
              ),
              "GlossSeeAlso" -> Json.arr(Json.fromString("GML"), Json.fromString("XML"))
            ),
            "GlossSee" -> Json.fromString("markup")
          )
        )
      )
    )
  )
}

package examples {
  case class Box[A](a: A)

  object Box {
    implicit def eqBox[A: Eq]: Eq[Box[A]] = Eq.by(_.a)
    implicit def arbitraryBox[A](implicit A: Arbitrary[A]): Arbitrary[Box[A]] = Arbitrary(A.arbitrary.map(Box(_)))
  }

  case class Qux[A](i: Int, a: A, j: Int)

  object Qux {
    implicit def eqQux[A: Eq]: Eq[Qux[A]] = Eq.by(q => (q.i, q.a, q.j))

    implicit def arbitraryQux[A](implicit A: Arbitrary[A]): Arbitrary[Qux[A]] =
      Arbitrary(
        for {
          i <- Arbitrary.arbitrary[Int]
          a <- A.arbitrary
          j <- Arbitrary.arbitrary[Int]
        } yield Qux(i, a, j)
      )
  }

  case class Wub(x: Long)

  object Wub {
    implicit val eqWub: Eq[Wub] = Eq.by(_.x)
    implicit val arbitraryWub: Arbitrary[Wub] = Arbitrary(Arbitrary.arbitrary[Long].map(Wub(_)))

    val decodeWub: Decoder[Wub] = Decoder[Long].prepare(_.downField("x")).map(Wub(_))
    val encodeWub: Encoder[Wub] = Encoder.instance(w => Json.obj("x" -> Json.fromLong(w.x)))
  }

  sealed trait Foo
  case class Bar(i: Int, s: String) extends Foo
  case class Baz(xs: List[String]) extends Foo
  case class Bam(w: Wub, d: Double) extends Foo

  sealed trait Mary
  case object HadA extends Mary
  case object LittleLamb extends Mary
  object Mary {
    implicit val eqMary: Eq[Mary] = Eq.fromUniversalEquals[Mary]
  }

  object Bar {
    implicit val eqBar: Eq[Bar] = Eq.fromUniversalEquals
    implicit val arbitraryBar: Arbitrary[Bar] = Arbitrary(
      for {
        i <- Arbitrary.arbitrary[Int]
        s <- Arbitrary.arbitrary[String]
      } yield Bar(i, s)
    )

    val decodeBar: Decoder[Bar] = Decoder.forProduct2("i", "s")(Bar.apply)
    val encodeBar: Encoder[Bar] = Encoder.forProduct2("i", "s") {
      case Bar(i, s) => (i, s)
    }
  }

  object Baz {
    implicit val eqBaz: Eq[Baz] = Eq.fromUniversalEquals
    implicit val arbitraryBaz: Arbitrary[Baz] = Arbitrary(
      Arbitrary.arbitrary[List[String]].map(Baz.apply)
    )

    implicit val decodeBaz: Decoder[Baz] = Decoder[List[String]].map(Baz(_))
    implicit val encodeBaz: Encoder[Baz] = Encoder.instance {
      case Baz(xs) => Json.fromValues(xs.map(Json.fromString))
    }
  }

  object Bam {
    implicit val eqBam: Eq[Bam] = Eq.fromUniversalEquals
    implicit val arbitraryBam: Arbitrary[Bam] = Arbitrary(
      for {
        w <- Arbitrary.arbitrary[Wub]
        d <- Arbitrary.arbitrary[Double]
      } yield Bam(w, d)
    )

    val decodeBam: Decoder[Bam] = Decoder.forProduct2("w", "d")(Bam.apply)(Wub.decodeWub, implicitly)
    val encodeBam: Encoder[Bam] = Encoder.forProduct2[Bam, Wub, Double]("w", "d") {
      case Bam(w, d) => (w, d)
    }(Wub.encodeWub, implicitly)
  }

  object Foo {
    implicit val eqFoo: Eq[Foo] = Eq.fromUniversalEquals

    implicit val arbitraryFoo: Arbitrary[Foo] = Arbitrary(
      Gen.oneOf(
        Arbitrary.arbitrary[Bar],
        Arbitrary.arbitrary[Baz],
        Arbitrary.arbitrary[Bam]
      )
    )

    val encodeFoo: Encoder[Foo] = Encoder.instance {
      case bar @ Bar(_, _) => Json.obj("Bar" -> Bar.encodeBar(bar))
      case baz @ Baz(_)    => Json.obj("Baz" -> Baz.encodeBaz(baz))
      case bam @ Bam(_, _) => Json.obj("Bam" -> Bam.encodeBam(bam))
    }

    val decodeFoo: Decoder[Foo] = Decoder.instance { c =>
      c.keys.map(_.toVector) match {
        case Some(Vector("Bar")) => c.get("Bar")(Bar.decodeBar.widen)
        case Some(Vector("Baz")) => c.get("Baz")(Baz.decodeBaz.widen)
        case Some(Vector("Bam")) => c.get("Bam")(Bam.decodeBam.widen)
        case _                   => Left(DecodingFailure("Foo", c.history))
      }
    }
  }
}
