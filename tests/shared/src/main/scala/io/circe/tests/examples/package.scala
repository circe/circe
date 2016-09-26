package io.circe.tests

import cats.Eq
import cats.instances.AllInstances
import io.circe.{ Decoder, Encoder, Json }
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

    implicit def arbitraryBox[A](implicit A: Arbitrary[A]): Arbitrary[Box[A]] =
      Arbitrary(A.arbitrary.map(Box(_)))
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
      case Baz(xs) => Json.fromValues(xs.map(Json.fromString))
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
