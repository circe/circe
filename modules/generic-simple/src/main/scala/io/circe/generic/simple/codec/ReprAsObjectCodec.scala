package io.circe.generic.simple.codec

import cats.Apply
import cats.data.Validated
import io.circe.{ Codec, Decoder, DecodingFailure, Encoder, HCursor, JsonObject }
import shapeless.{ :+:, ::, CNil, Coproduct, HList, HNil, Inl, Inr, Lazy, Witness }
import shapeless.labelled.{ FieldType, field }

/**
 * A codec for a generic representation of a case class or ADT.
 *
 * Note that users typically will not work with instances of this class.
 */
abstract class ReprAsObjectCodec[A] extends Codec.AsObject[A]

final object ReprAsObjectCodec extends LowPriorityReprCodecInstances {
  implicit val codecForHNil: ReprAsObjectCodec[HNil] = new ReprAsObjectCodec[HNil] {
    def apply(c: HCursor): Decoder.Result[HNil] = Right(HNil)
    def encodeObject(a: HNil): JsonObject = JsonObject.empty
  }

  implicit def codecForHCons[K <: Symbol, H, T <: HList](
    implicit
    key: Witness.Aux[K],
    decodeH: Decoder[H],
    encodeH: Encoder[H],
    codecForT: ReprAsObjectCodec[T]
  ): ReprAsObjectCodec[FieldType[K, H] :: T] = new ReprAsObjectCodec[FieldType[K, H] :: T] {
    def apply(c: HCursor): Decoder.Result[FieldType[K, H] :: T] = for {
      h <- c.get(key.value.name)(decodeH)
      t <- codecForT(c)
    } yield field[K](h) :: t

    override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[FieldType[K, H] :: T] =
      consResults[Decoder.AccumulatingResult, K, H, T](
        decodeH.tryDecodeAccumulating(c.downField(key.value.name)),
        codecForT.decodeAccumulating(c)
      )

    def encodeObject(a: FieldType[K, H] :: T): JsonObject = a match {
      case h :: t => ((key.value.name, encodeH(h))) +: codecForT.encodeObject(t)
    }
  }

  implicit val codecForCNil: ReprAsObjectCodec[CNil] = new ReprAsObjectCodec[CNil] {
    def apply(c: HCursor): Decoder.Result[CNil] = Left(DecodingFailure("CNil", c.history))
    def encodeObject(a: CNil): JsonObject =
      sys.error("No JSON representation of CNil (this shouldn't happen)")
  }

  implicit def codecForCoproduct[K <: Symbol, L, R <: Coproduct](
    implicit
    key: Witness.Aux[K],
    decodeL: Decoder[L],
    encodeL: Encoder[L],
    codecForR: ReprAsObjectCodec[R]
  ): ReprAsObjectCodec[FieldType[K, L] :+: R] = new ReprAsObjectCodec[FieldType[K, L] :+: R] {
    def apply(c: HCursor): Decoder.Result[FieldType[K, L] :+: R] =
      c.downField(key.value.name).focus match {
        case Some(value) => value.as(decodeL).map(l => Inl(field(l)))
        case None        => codecForR(c).map(Inr(_))
      }

    override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[FieldType[K, L] :+: R] = {
      val f = c.downField(key.value.name)

      f.focus match {
        case Some(value) => decodeL.tryDecodeAccumulating(f).map(l => Inl(field(l)))
        case None        => codecForR.decodeAccumulating(c).map(Inr(_))
      }
    }

    def encodeObject(a: FieldType[K, L] :+: R): JsonObject = a match {
      case Inl(l) => JsonObject.singleton(key.value.name, encodeL(l))
      case Inr(r) => codecForR.encodeObject(r)
    }
  }

}

trait LowPriorityReprCodecInstances {
  def consResults[F[_], K, V, T <: HList](hv: F[V], tr: F[T])(
    implicit F: Apply[F]
  ): F[FieldType[K, V] :: T] =
    F.map2(hv, tr)((v, t) => field[K].apply[V](v) :: t)

  def injectLeftValue[K, V, R <: Coproduct](v: V): FieldType[K, V] :+: R = Inl(field[K].apply[V](v))

  val hnilResult: Decoder.Result[HNil] = Right(HNil)
  val hnilResultAccumulating: Decoder.AccumulatingResult[HNil] = Validated.valid(HNil)

  implicit def codecForHConsDerived[K <: Symbol, H, T <: HList](
    implicit
    key: Witness.Aux[K],
    codecForH: Lazy[DerivedAsObjectCodec[H]],
    codecForT: Lazy[ReprAsObjectCodec[T]]
  ): ReprAsObjectCodec[FieldType[K, H] :: T] = new ReprAsObjectCodec[FieldType[K, H] :: T] {
    def apply(c: HCursor): Decoder.Result[FieldType[K, H] :: T] = for {
      h <- c.get(key.value.name)(codecForH.value)
      t <- codecForT.value(c)
    } yield field[K](h) :: t

    override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[FieldType[K, H] :: T] =
      consResults[Decoder.AccumulatingResult, K, H, T](
        codecForH.value.tryDecodeAccumulating(c.downField(key.value.name)),
        codecForT.value.decodeAccumulating(c)
      )

    def encodeObject(a: FieldType[K, H] :: T): JsonObject = a match {
      case h :: t => ((key.value.name, codecForH.value(h))) +: codecForT.value.encodeObject(t)
    }
  }

  implicit def codecForCoproductDerived[K <: Symbol, L, R <: Coproduct](
    implicit
    key: Witness.Aux[K],
    codecForL: Lazy[DerivedAsObjectCodec[L]],
    codecForR: Lazy[ReprAsObjectCodec[R]]
  ): ReprAsObjectCodec[FieldType[K, L] :+: R] = new ReprAsObjectCodec[FieldType[K, L] :+: R] {
    def apply(c: HCursor): Decoder.Result[FieldType[K, L] :+: R] =
      c.downField(key.value.name).focus match {
        case Some(value) => value.as(codecForL.value).map(l => Inl(field(l)))
        case None        => codecForR.value(c).map(Inr(_))
      }

    override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[FieldType[K, L] :+: R] = {
      val f = c.downField(key.value.name)

      f.focus match {
        case Some(value) => codecForL.value.tryDecodeAccumulating(f).map(l => Inl(field(l)))
        case None        => codecForR.value.decodeAccumulating(c).map(Inr(_))
      }
    }

    def encodeObject(a: FieldType[K, L] :+: R): JsonObject = a match {
      case Inl(l) => JsonObject.singleton(key.value.name, codecForL.value(l))
      case Inr(r) => codecForR.value.encodeObject(r)
    }
  }
}
