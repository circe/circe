package io.circe.shapes

import cats.data.Validated
import cats.kernel.Eq
import io.circe.{ Decoder, DecodingFailure, Encoder, HCursor, JsonObject, KeyDecoder, KeyEncoder }
import shapeless.{ ::, HList, Widen, Witness }
import shapeless.labelled.{ FieldType, field }

trait LabelledHListInstances extends LowPriorityLabelledHListInstances {

  /**
   * Decode a record element with a symbol key.
   *
   * This is provided as a special case because of type inference issues with
   * `decodeRecord` for symbols.
   */
  implicit final def decodeSymbolLabelledHCons[K <: Symbol, V, T <: HList](
    implicit
    witK: Witness.Aux[K],
    decodeV: Decoder[V],
    decodeT: Decoder[T]
  ): Decoder[FieldType[K, V] :: T] = new Decoder[FieldType[K, V] :: T] {
    def apply(c: HCursor): Decoder.Result[FieldType[K, V] :: T] = Decoder.resultInstance.map2(
      c.get[V](witK.value.name),
      decodeT(c)
    )((h, t) => field[K](h) :: t)

    override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[FieldType[K, V] :: T] =
      Decoder.accumulatingResultInstance.map2(
        decodeV.tryDecodeAccumulating(c.downField(witK.value.name)),
        decodeT.decodeAccumulating(c)
      )((h, t) => field[K](h) :: t)
  }

  /**
   * Encode a record element with a symbol key.
   *
   * This is provided as a special case because of type inference issues with
   * `encodeRecord` for symbols.
   */
  implicit final def encodeSymbolLabelledHCons[K <: Symbol, V, T <: HList](
    implicit
    witK: Witness.Aux[K],
    encodeV: Encoder[V],
    encodeT: Encoder.AsObject[T]
  ): Encoder.AsObject[FieldType[K, V] :: T] = new Encoder.AsObject[FieldType[K, V] :: T] {
    def encodeObject(a: FieldType[K, V] :: T): JsonObject =
      encodeT.encodeObject(a.tail).add(witK.value.name, encodeV(a.head))
  }
}

private[shapes] trait LowPriorityLabelledHListInstances extends HListInstances {
  implicit final def decodeLabelledHCons[K, W >: K, V, T <: HList](
    implicit
    witK: Witness.Aux[K],
    widenK: Widen.Aux[K, W],
    eqW: Eq[W],
    decodeW: KeyDecoder[W],
    decodeV: Decoder[V],
    decodeT: Decoder[T]
  ): Decoder[FieldType[K, V] :: T] = new Decoder[FieldType[K, V] :: T] {
    private[this] val widened = widenK(witK.value)
    private[this] val isK: String => Boolean = decodeW(_).exists(eqW.eqv(widened, _))

    def apply(c: HCursor): Decoder.Result[FieldType[K, V] :: T] = Decoder.resultInstance.map2(
      c.keys
        .flatMap(_.find(isK))
        .fold[Decoder.Result[String]](
          Left(DecodingFailure("Record", c.history))
        )(Right(_))
        .flatMap(c.get[V](_)),
      decodeT(c)
    )((h, t) => field[K](h) :: t)

    override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[FieldType[K, V] :: T] =
      Decoder.accumulatingResultInstance.map2(
        c.keys
          .flatMap(_.find(isK))
          .fold[Decoder.AccumulatingResult[String]](
            Validated.invalidNel(DecodingFailure("Record", c.history))
          )(Validated.valid)
          .andThen(k => decodeV.tryDecodeAccumulating(c.downField(k))),
        decodeT.decodeAccumulating(c)
      )((h, t) => field[K](h) :: t)
  }

  implicit final def encodeLabelledHCons[K, W >: K, V, T <: HList](
    implicit
    witK: Witness.Aux[K],
    widenK: Widen.Aux[K, W],
    encodeW: KeyEncoder[W],
    encodeV: Encoder[V],
    encodeT: Encoder.AsObject[T]
  ): Encoder.AsObject[FieldType[K, V] :: T] = new Encoder.AsObject[FieldType[K, V] :: T] {
    private[this] val widened = widenK(witK.value)

    def encodeObject(a: FieldType[K, V] :: T): JsonObject =
      encodeT.encodeObject(a.tail).add(encodeW(widened), encodeV(a.head))
  }
}
