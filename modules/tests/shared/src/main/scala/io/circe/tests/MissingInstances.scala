package io.circe.tests

import cats.Eq
import java.util.UUID
import org.scalacheck.{ Arbitrary, Gen }
import org.scalacheck.util.Buildable
import shapeless.{ ::, AdditiveCollection, Generic, HList, HNil, IsTuple, Nat, Sized }
import shapeless.labelled.{ field, FieldType }
import shapeless.ops.nat.ToInt

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

  implicit def eqHCons[H, T <: HList](implicit eqH: Eq[H], eqT: Eq[T]): Eq[H :: T] =
    Eq.instance[H :: T] {
      case (h1 :: t1, h2 :: t2) => eqH.eqv(h1, h2) && eqT.eqv(t1, t2)
    }

  implicit def eqTuple[P: IsTuple, L <: HList](implicit
    gen: Generic.Aux[P, L],
    eqL: Eq[L]
  ): Eq[P] = eqL.on(gen.to)

  implicit lazy val arbitraryHNil: Arbitrary[HNil] = Arbitrary(Gen.const(HNil))

  implicit def arbitraryHCons[H, T <: HList](implicit H: Arbitrary[H], T: Arbitrary[T]): Arbitrary[H :: T] =
    Arbitrary(
      for {
        h <- H.arbitrary
        t <- T.arbitrary
      } yield h :: t
    )

  implicit def eqFieldType[K, V](implicit V: Eq[V]): Eq[FieldType[K, V]] = V.on(identity)

  implicit def arbitraryFieldType[K, V](implicit V: Arbitrary[V]): Arbitrary[FieldType[K, V]] =
    Arbitrary(V.arbitrary.map(field[K](_)))

  implicit def eqSized[L <: Nat, C[_], A](implicit CA: Eq[C[A]]): Eq[Sized[C[A], L]] = CA.on(_.unsized)

  implicit def arbitrarySized[L <: Nat, C[_], A](implicit
    A: Arbitrary[A],
    additive: AdditiveCollection[C[A]],
    buildable: Buildable[A, C[A]],
    ev: C[A] => Traversable[A],
    toInt: ToInt[L]
  ): Arbitrary[Sized[C[A], L]] =
    Arbitrary(Gen.containerOfN[C, A](toInt(), A.arbitrary).filter(_.size == toInt()).map(Sized.wrap[C[A], L]))
}
