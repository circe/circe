package io.circe.generic.simple.decoding

import cats.Apply
import cats.data.Validated
import io.circe.{ Decoder, DecodingFailure, HCursor }
import shapeless.{ :+:, ::, CNil, Coproduct, HList, HNil, Inl, Inr, Witness }
import shapeless.labelled.{ FieldType, field }

/**
 * A decoder for a generic representation of a case class or ADT.
 *
 * Note that users typically will not work with instances of this class.
 */
abstract class ReprDecoder[A] extends Decoder[A]

object ReprDecoder {
  private[this] def consResults[F[_], K, V, T <: HList](hv: F[V], tr: F[T])(
    implicit F: Apply[F]
  ): F[FieldType[K, V] :: T] =
    F.map2(hv, tr)((v, t) => field[K].apply[V](v) :: t)

  implicit val decodeHNil: ReprDecoder[HNil] = new ReprDecoder[HNil] {
    def apply(c: HCursor): Decoder.Result[HNil] = Right(HNil)
  }

  implicit def decodeHCons[K <: Symbol, H, T <: HList](
    implicit
    key: Witness.Aux[K],
    decodeH: Decoder[H],
    decodeT: ReprDecoder[T]
  ): ReprDecoder[FieldType[K, H] :: T] = new ReprDecoder[FieldType[K, H] :: T] {
    def apply(c: HCursor): Decoder.Result[FieldType[K, H] :: T] = for {
      h <- c.get(key.value.name)(decodeH)
      t <- decodeT(c)
    } yield field[K](h) :: t

    override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[FieldType[K, H] :: T] =
      consResults[Decoder.AccumulatingResult, K, H, T](
        decodeH.tryDecodeAccumulating(c.downField(key.value.name)),
        decodeT.decodeAccumulating(c)
      )
  }

  implicit val decodeCNil: ReprDecoder[CNil] = new ReprDecoder[CNil] {
    def apply(c: HCursor): Decoder.Result[CNil] = Left(DecodingFailure("CNil", c.history))
  }

  implicit def decodeCoproduct[K <: Symbol, L, R <: Coproduct](
    implicit
    key: Witness.Aux[K],
    decodeL: Decoder[L],
    decodeR: => ReprDecoder[R]
  ): ReprDecoder[FieldType[K, L] :+: R] = new ReprDecoder[FieldType[K, L] :+: R] {
    private[this] lazy val cachedDecodeR: Decoder[R] = decodeR

    def apply(c: HCursor): Decoder.Result[FieldType[K, L] :+: R] =
      c.downField(key.value.name).focus match {
        case Some(value) => value.as(decodeL).map(l => Inl(field(l)))
        case None        => cachedDecodeR(c).map(Inr(_))
      }

    override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[FieldType[K, L] :+: R] = {
      val f = c.downField(key.value.name)

      f.focus match {
        case Some(value) => decodeL.tryDecodeAccumulating(f).map(l => Inl(field(l)))
        case None        => cachedDecodeR.decodeAccumulating(c).map(Inr(_))
      }
    }
  }
}
