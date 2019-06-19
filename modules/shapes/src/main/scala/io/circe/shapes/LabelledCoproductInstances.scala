package io.circe.shapes

import cats.kernel.Eq
import io.circe.{ Decoder, DecodingFailure, Encoder, HCursor, Json, KeyDecoder, KeyEncoder }
import shapeless.{ :+:, Coproduct, Inl, Inr, Widen, Witness }
import shapeless.labelled.{ FieldType, field }

trait LabelledCoproductInstances extends LowPriorityLabelledCoproductInstances {
  implicit final def decodeSymbolLabelledCCons[K <: Symbol, V, R <: Coproduct](
    implicit
    witK: Witness.Aux[K],
    decodeV: Decoder[V],
    decodeR: Decoder[R]
  ): Decoder[FieldType[K, V] :+: R] = new Decoder[FieldType[K, V] :+: R] {
    def apply(c: HCursor): Decoder.Result[FieldType[K, V] :+: R] =
      Decoder.resultSemigroupK.combineK(
        c.get[V](witK.value.name).map(v => Inl(field[K](v))),
        decodeR(c).map(Inr(_))
      )
  }

  implicit final def encodeSymbolLabelledCCons[K <: Symbol, V, R <: Coproduct](
    implicit
    witK: Witness.Aux[K],
    encodeV: Encoder[V],
    encodeR: Encoder[R]
  ): Encoder[FieldType[K, V] :+: R] = new Encoder[FieldType[K, V] :+: R] {
    def apply(a: FieldType[K, V] :+: R): Json = a match {
      case Inl(l) => Json.obj((witK.value.name, encodeV(l)))
      case Inr(r) => encodeR(r)
    }
  }
}

private[shapes] trait LowPriorityLabelledCoproductInstances extends CoproductInstances {
  implicit final def decodeLabelledCCons[K, W >: K, V, R <: Coproduct](
    implicit
    witK: Witness.Aux[K],
    widenK: Widen.Aux[K, W],
    eqW: Eq[W],
    decodeW: KeyDecoder[W],
    decodeV: Decoder[V],
    decodeR: Decoder[R]
  ): Decoder[FieldType[K, V] :+: R] = new Decoder[FieldType[K, V] :+: R] {
    private[this] val widened = widenK(witK.value)
    private[this] val isK: String => Boolean = decodeW(_).exists(eqW.eqv(widened, _))

    def apply(c: HCursor): Decoder.Result[FieldType[K, V] :+: R] =
      Decoder.resultSemigroupK.combineK(
        c.keys
          .flatMap(_.find(isK))
          .fold[Decoder.Result[String]](
            Left(DecodingFailure("Record", c.history))
          )(Right(_))
          .flatMap(c.get[V](_).map(v => Inl(field[K](v)))),
        decodeR(c).map(Inr(_))
      )
  }

  implicit final def encodeLabelledCCons[K, W >: K, V, R <: Coproduct](
    implicit
    witK: Witness.Aux[K],
    eqW: Eq[W],
    encodeW: KeyEncoder[W],
    encodeV: Encoder[V],
    encodeR: Encoder[R]
  ): Encoder[FieldType[K, V] :+: R] = new Encoder[FieldType[K, V] :+: R] {
    def apply(a: FieldType[K, V] :+: R): Json = a match {
      case Inl(l) => Json.obj((encodeW(witK.value), encodeV(l)))
      case Inr(r) => encodeR(r)
    }
  }
}
