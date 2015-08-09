package io.circe.test

import algebra.Eq
import org.scalacheck.{ Arbitrary, Gen }

trait MissingInstances {
  implicit def eqBigDecimal: Eq[BigDecimal] = Eq.fromUniversalEquals
  implicit def eqTuple1[A: Eq]: Eq[Tuple1[A]] = Eq.by(_._1)
  implicit def eqTuple3[A: Eq, B: Eq, C: Eq]: Eq[(A, B, C)] = Eq.instance {
    case ((a1, b1, c1), (a2, b2, c2)) => Eq[A].eqv(a1, a2) && Eq[B].eqv(b1, b2) && Eq[C].eqv(c1, c2)
  }

  implicit def arbitraryTuple1[A](implicit A: Arbitrary[A]): Arbitrary[Tuple1[A]] =
    Arbitrary(A.arbitrary.map(Tuple1(_)))
}
