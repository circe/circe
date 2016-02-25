package io.circe.tests

import algebra.Eq
import java.util.UUID
import org.scalacheck.Arbitrary
import shapeless._

trait MissingInstances {
  implicit def eqBigDecimal: Eq[BigDecimal] = Eq.fromUniversalEquals

  implicit def eqUUID: Eq[UUID] = Eq.fromUniversalEquals

  implicit def arbitraryTuple1[A](implicit A: Arbitrary[A]): Arbitrary[Tuple1[A]] =
    Arbitrary(A.arbitrary.map(Tuple1(_)))

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
