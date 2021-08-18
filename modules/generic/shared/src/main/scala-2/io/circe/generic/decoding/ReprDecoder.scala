package io.circe.generic.decoding

import cats.Apply
import cats.data.Validated
import io.circe.DecodingFailure.Reason.WrongTypeExpectation
import io.circe.{ Decoder, DecodingFailure, HCursor }
import io.circe.generic.Deriver

import scala.language.experimental.macros
import shapeless.{ :+:, ::, Coproduct, HList, HNil, Inl }
import shapeless.labelled.{ FieldType, field }

/**
 * A decoder for a generic representation of a case class or ADT.
 *
 * Note that users typically will not work with instances of this class.
 */
abstract class ReprDecoder[A] extends Decoder[A]

object ReprDecoder {
  implicit def deriveReprDecoder[R]: ReprDecoder[R] = macro Deriver.deriveDecoder[R]

  val hnilReprDecoder: ReprDecoder[HNil] = new ReprDecoder[HNil] {
    def apply(c: HCursor): Decoder.Result[HNil] =
      if (c.value.isObject) Right(HNil)
      else Left(DecodingFailure(WrongTypeExpectation("object", c.value), c.history))
  }

  def consResults[F[_], K, V, T <: HList](hv: F[V], tr: F[T])(implicit F: Apply[F]): F[FieldType[K, V] :: T] =
    F.map2(hv, tr)((v, t) => field[K].apply[V](v) :: t)

  def injectLeftValue[K, V, R <: Coproduct](v: V): FieldType[K, V] :+: R = Inl(field[K].apply[V](v))

  val hnilResult: Decoder.Result[HNil] = Right(HNil)
  val hnilResultAccumulating: Decoder.AccumulatingResult[HNil] = Validated.valid(HNil)
}
