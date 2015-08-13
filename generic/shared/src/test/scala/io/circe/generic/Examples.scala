package io.circe.generic

import algebra.Eq
import io.circe.test.CirceSuite
import org.scalacheck.{ Arbitrary, Gen }

trait Examples { this: CirceSuite =>
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
}
