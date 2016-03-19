package io.circe.tests

import algebra.Eq
import java.util.UUID
import org.scalacheck.{ Arbitrary, Gen }
import shapeless._

trait MissingInstances {
  implicit lazy val eqThrowable: Eq[Throwable] = Eq.fromUniversalEquals
  implicit lazy val eqBigDecimal: Eq[BigDecimal] = Eq.fromUniversalEquals
  implicit lazy val eqUUID: Eq[UUID] = Eq.fromUniversalEquals

  implicit def arbitraryTuple1[A](implicit A: Arbitrary[A]): Arbitrary[Tuple1[A]] =
    Arbitrary(A.arbitrary.map(Tuple1(_)))

  implicit def arbitrarySome[A](implicit A: Arbitrary[A]): Arbitrary[Some[A]] = Arbitrary(A.arbitrary.map(Some(_)))
  implicit lazy val arbitraryNone: Arbitrary[None.type] = Arbitrary(Gen.const(None))

  implicit def eqSome[A](implicit A: Eq[A]): Eq[Some[A]] = Eq.by(_.x)
  implicit lazy val eqNone: Eq[None.type] = Eq.instance((_, _) => true)

  implicit lazy val arbitrarySymbol: Arbitrary[Symbol] = Arbitrary(Arbitrary.arbitrary[String].map(Symbol(_)))

  implicit lazy val eqHNil: Eq[HNil] = Eq.instance((_, _) => true)

  implicit def eqTupleHCons[H, T <: HList](implicit eqH: Eq[H], eqT: Eq[T]): Eq[H :: T] =
    Eq.instance[H :: T] {
      case (h1 :: t1, h2 :: t2) => eqH.eqv(h1, h2) && eqT.eqv(t1, t2)
    }

  implicit def eqTuple[P: IsTuple, L <: HList](implicit
    gen: Generic.Aux[P, L],
    eqL: Eq[L]
  ): Eq[P] = eqL.on(gen.to)
}
