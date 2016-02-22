package io.circe.generic

import algebra.Eq
import cats.std.AllInstances
import io.circe.generic.jsoncodecmacrossuiteaux._
import io.circe.tests.{ ArbitraryInstances, CirceSuite, CodecTests, MissingInstances }
import org.scalacheck.{ Arbitrary, Gen }

package object jsoncodecmacrossuiteaux extends AnyRef
  with AllInstances with ArbitraryInstances with MissingInstances

package jsoncodecmacrossuiteaux {

  // Simple

  @JsonCodec final case class Simple(i: Int, l: Long, s: String)

  object Simple {
    implicit def eqSimple: Eq[Simple] = Eq.by(s => (s.i, s.l, s.s))

    implicit def arbitrarySimple: Arbitrary[Simple] =
      Arbitrary(
        for {
          i <- Arbitrary.arbitrary[Int]
          l <- Arbitrary.arbitrary[Long]
          s <- Arbitrary.arbitrary[String]
        } yield Simple(i, l, s)
      )
  }

  // Single

  @JsonCodec final case class Single(i: Int)

  object Single {
    implicit def eqSingle: Eq[Single] = Eq.by(_.i)

    implicit def arbitrarySingle: Arbitrary[Single] =
      Arbitrary(Arbitrary.arbitrary[Int].map(Single(_)))
  }

  // Typed1

  @JsonCodec final case class Typed1[A](i: Int, a: A, j: Int)

  object Typed1 {
    implicit def eqTyped1[A: Eq]: Eq[Typed1[A]] = Eq.by(t => (t.i, t.a, t.j))

    implicit def arbitraryTyped1[A](implicit A: Arbitrary[A]): Arbitrary[Typed1[A]] =
      Arbitrary(
        for {
          i <- Arbitrary.arbitrary[Int]
          a <- A.arbitrary
          j <- Arbitrary.arbitrary[Int]
        } yield Typed1(i, a, j)
      )
  }

  // Typed2

  @JsonCodec final case class Typed2[A, B](i: Int, a: A, b: B, j: Int)

  object Typed2 {
    implicit def eqTyped2[A: Eq, B: Eq]: Eq[Typed2[A, B]] = Eq.by(t => (t.i, t.a, t.b, t.j))

    implicit def arbitraryTyped2[A, B](implicit A: Arbitrary[A], B: Arbitrary[B])
    : Arbitrary[Typed2[A, B]] =
      Arbitrary(
        for {
          i <- Arbitrary.arbitrary[Int]
          a <- A.arbitrary
          b <- B.arbitrary
          j <- Arbitrary.arbitrary[Int]
        } yield Typed2(i, a, b, j)
      )
  }

  // Hierarchy

  @JsonCodec sealed trait Hierarchy
  final case class Hierarchy1(i: Int, s: String) extends Hierarchy
  final case class Hierarchy2(xs: List[String]) extends Hierarchy
  final case class Hierarchy3(s: Single, d: Double) extends Hierarchy

  object Hierarchy {
    implicit val eqFoo: Eq[Hierarchy] = Eq.fromUniversalEquals

    implicit val arbitraryFoo: Arbitrary[Hierarchy] = Arbitrary(
      Gen.oneOf(
        for {
          i <- Arbitrary.arbitrary[Int]
          s <- Arbitrary.arbitrary[String]
        } yield Hierarchy1(i, s),
        Gen.listOf(Arbitrary.arbitrary[String]).map(Hierarchy2.apply),
        for {
          s <- Arbitrary.arbitrary[Single]
          d <- Arbitrary.arbitrary[Double]
        } yield Hierarchy3(s, d)
      )
    )
  }

  // RecursiveHierarchy

  @JsonCodec sealed trait RecursiveHierarchy
  final case class BaseRecursiveHierarchy(a: String) extends RecursiveHierarchy
  final case class NestedRecursiveHierarchy(r: RecursiveHierarchy) extends RecursiveHierarchy

  object RecursiveHierarchy {
    implicit val eqRecursiveAdtExample: Eq[RecursiveHierarchy] = Eq.fromUniversalEquals

    private def atDepth(depth: Int): Gen[RecursiveHierarchy] = if (depth < 3)
      Gen.oneOf(
        Arbitrary.arbitrary[String].map(BaseRecursiveHierarchy(_)),
        atDepth(depth + 1).map(NestedRecursiveHierarchy(_))
      ) else Arbitrary.arbitrary[String].map(BaseRecursiveHierarchy(_))

    implicit val arbitraryRecursiveAdtExample: Arbitrary[RecursiveHierarchy] =
      Arbitrary(atDepth(0))
  }

  // SelfRecursiveWithOption

  @JsonCodec final case class SelfRecursiveWithOption(o: Option[SelfRecursiveWithOption])

  object SelfRecursiveWithOption {
    implicit val eqSelfRecursiveWithOption: Eq[SelfRecursiveWithOption] = Eq.fromUniversalEquals

    private def atDepth(depth: Int): Gen[SelfRecursiveWithOption] = if (depth < 3)
      Arbitrary.arbitrary[Option[SelfRecursiveWithOption]].map(
        SelfRecursiveWithOption(_)
      ) else Gen.const(SelfRecursiveWithOption(None))

    implicit val arbitrarySelfRecursiveWithOption: Arbitrary[SelfRecursiveWithOption] =
      Arbitrary(atDepth(0))
  }
}

class JsonCodecMacrosSuite extends CirceSuite {
  checkAll("Codec[Simple]", CodecTests[Simple].codec)
  checkAll("Codec[Single]", CodecTests[Single].codec)
  checkAll("Codec[Typed1[Int]]", CodecTests[Typed1[Int]].codec)
  checkAll("Codec[Typed2[Int, Long]]", CodecTests[Typed2[Int, Long]].codec)
  checkAll("Codec[Hierarchy]", CodecTests[Hierarchy].codec)
  checkAll("Codec[RecursiveHierarchy]", CodecTests[RecursiveHierarchy].codec)
  checkAll("Codec[SelfRecursiveWithOption]", CodecTests[SelfRecursiveWithOption].codec)
}
