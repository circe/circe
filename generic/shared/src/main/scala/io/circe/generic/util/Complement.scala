package io.circe.generic.util

import shapeless._
import shapeless.labelled.{ FieldType, field }

/**
 * Supports removal and insertion of an element (possibly unlabeled) into an `HList`.
 *
 * @author Travis Brown
 */
trait Insert[L <: HList, E] extends DepFn1[L] with Serializable {
  def insert(e: E, out: Out): L
}

trait LowPriorityInsert {
  type Aux[L <: HList, E, Out0] = Insert[L, E] {
    type Out = Out0
  }

  implicit def insertTail[H, T <: HList, E, OutT <: HList](implicit
    insertT: Aux[T, E, OutT]
  ): Aux[H :: T, E, H :: OutT] = new Insert[H :: T, E] {
    type Out = H :: OutT

    def apply(l: H :: T): Out = l.head :: insertT(l.tail)
    def insert(e: E, out: H :: OutT): H :: T =
      out.head :: insertT.insert(e, out.tail)
  }
}

object Insert extends LowPriorityInsert {
  def apply[L <: HList, E](implicit
    insert: Insert[L, E]
  ): Aux[L, E, insert.Out] = insert

  implicit def insertHead[H, T <: HList]: Aux[H :: T, H, T] =
    new Insert[H :: T, H] {
      type Out = T

      def apply(l: H :: T): Out = l.tail
      def insert(e: H, out: T): H :: T = e :: out
    }

  implicit def insertUnlabeledHead[H, K, T <: HList]: Aux[FieldType[K, H] :: T, H, T] =
    new Insert[FieldType[K, H] :: T, H] {
      type Out = T

      def apply(l: FieldType[K, H] :: T): Out = l.tail
      def insert(e: H, out: T): FieldType[K, H] :: T = field[K](e) :: out
    }
}

/**
 * Supports removal and insertion of an `HList` of elements (possibly unlabeled) into an `HList`.
 *
 * @author Travis Brown
 */
trait Complement[L <: HList, A <: HList] extends DepFn1[L] {
  def insert(a: A, out: Out): L
}

object Complement {
  type Aux[L <: HList, A <: HList, Out0] = Complement[L, A] {
    type Out = Out0
  }

  def apply[L <: HList, A <: HList](implicit
    complement: Complement[L, A]
  ): Aux[L, A, complement.Out] = complement

  implicit def hnilCompl[L <: HList]: Aux[L, HNil, L] =
    new Complement[L, HNil] {
      type Out = L

      def apply(l: L): L = l
      def insert(a: HNil, out: L): L = out
    }

  implicit def hconsCompl[L <: HList, H, T <: HList, OutT <: HList](implicit
    complementT: Complement.Aux[L, T, OutT],
    insertH: Insert[OutT, H]
  ): Aux[L, H :: T, insertH.Out] = new Complement[L, H :: T] {
    type Out = insertH.Out

    def apply(l: L): insertH.Out = insertH(complementT(l))
    def insert(a: H :: T, out: insertH.Out): L =
      complementT.insert(a.tail, insertH.insert(a.head, out))
  }
}
