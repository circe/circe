package io.circe.tests

import algebra.Eq
import cats.std.AllInstances
import io.circe.{ Decoder, Encoder, Json }
import org.scalacheck.{ Arbitrary, Gen }

package object examples extends AllInstances with ArbitraryInstances with MissingInstances {
  val glossary: Json = Json.obj(
    "glossary" -> Json.obj(
      "title" -> Json.string("example glossary"),
      "GlossDiv" -> Json.obj(
        "title" -> Json.string("S"),
        "GlossList" -> Json.obj(
          "GlossEntry" -> Json.obj(
            "ID" -> Json.string("SGML"),
            "SortAs" -> Json.string("SGML"),
            "GlossTerm" -> Json.string("Standard Generalized Markup Language"),
            "Acronym" -> Json.string("SGML"),
            "Abbrev" -> Json.string("ISO 8879:1986"),
            "GlossDef" -> Json.obj(
              "para" -> Json.string(
                "A meta-markup language, used to create markup languages such as DocBook."
              ),
              "GlossSeeAlso" -> Json.array(Json.string("GML"), Json.string("XML"))
            ),
            "GlossSee" -> Json.string("markup")
          )
        )
      )
    )
  )
}

package examples {
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

  object Baz {
    implicit val decodeBaz: Decoder[Baz] = Decoder[List[String]].map(Baz(_))
    implicit val encodeBaz: Encoder[Baz] = Encoder.instance {
      case Baz(xs) => Json.fromValues(xs.map(Json.string))
    }
  }

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
}
