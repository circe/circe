package io.circe.generic.decoding

import cats.data.Xor
import cats.syntax.cartesian._
import io.circe.{ AccumulatingDecoder, Decoder, DecodingFailure, HCursor }
import shapeless._, shapeless.labelled.{ FieldType, field }

trait DerivedDecoder[A] extends Decoder[A]

final object DerivedDecoder extends IncompleteDerivedDecoders with LowPriorityDerivedDecoders {
  final def fromDecoder[A](decode: Decoder[A]): DerivedDecoder[A] = new DerivedDecoder[A] {
    final def apply(c: HCursor): Decoder.Result[A] = decode(c)
    override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] =
      decode.decodeAccumulating(c)
  }

  final implicit val decodeHNil: DerivedDecoder[HNil] =
    new DerivedDecoder[HNil] {
      final def apply(c: HCursor): Decoder.Result[HNil] = Xor.right(HNil)
    }

  implicit final val decodeCNil: DerivedDecoder[CNil] =
    new DerivedDecoder[CNil] {
      final def apply(c: HCursor): Decoder.Result[CNil] =
        Xor.left(DecodingFailure("CNil", c.history))
    }

  implicit final def decodeCoproduct[K <: Symbol, H, T <: Coproduct](implicit
    key: Witness.Aux[K],
    decodeHead: Lazy[Decoder[H]],
    decodeTail: Lazy[DerivedDecoder[T]]
  ): DerivedDecoder[FieldType[K, H] :+: T] = new DerivedDecoder[FieldType[K, H] :+: T] {
    final def apply(c: HCursor): Decoder.Result[FieldType[K, H] :+: T] =
      c.downField(key.value.name).focus.fold[Xor[DecodingFailure, FieldType[K, H] :+: T]](
        decodeTail.value(c).map(Inr(_))
      ) { headJson =>
        headJson.as(decodeHead.value).map(h => Inl(field(h)))
      }
  }

  implicit final def decodeLabelledHList[K <: Symbol, H, T <: HList](implicit
    key: Witness.Aux[K],
    decodeHead: Lazy[Decoder[H]],
    decodeTail: Lazy[DerivedDecoder[T]]
  ): DerivedDecoder[FieldType[K, H] :: T] = fromDecoder(
    (decodeHead.value.prepare(_.downField(key.value.name)) |@| decodeTail.value).map(
      (head, tail) => field[K](head) :: tail
    )
  )
}

private[circe] trait LowPriorityDerivedDecoders {
  implicit final def decodeCoproductDerived[K <: Symbol, H, T <: Coproduct](implicit
    key: Witness.Aux[K],
    decodeHead: Lazy[DerivedDecoder[H]],
    decodeTail: Lazy[DerivedDecoder[T]]
  ): DerivedDecoder[FieldType[K, H] :+: T] = new DerivedDecoder[FieldType[K, H] :+: T] {
    final def apply(c: HCursor): Decoder.Result[FieldType[K, H] :+: T] =
      c.downField(key.value.name).focus.fold[Xor[DecodingFailure, FieldType[K, H] :+: T]](
        decodeTail.value(c).map(Inr(_))
      ) { headJson =>
        headJson.as(decodeHead.value).map(h => Inl(field(h)))
      }
  }

  implicit final def decodeAdt[A, R <: Coproduct](implicit
    gen: LabelledGeneric.Aux[A, R],
    decode: Lazy[DerivedDecoder[R]]
  ): DerivedDecoder[A] = new DerivedDecoder[A] {
    final def apply(c: HCursor): Decoder.Result[A] = decode.value(c).map(gen.from)
  }

  implicit final def decodeCaseClass[A, R <: HList](implicit
    gen: LabelledGeneric.Aux[A, R],
    decode: Lazy[DerivedDecoder[R]]
  ): DerivedDecoder[A] = new DerivedDecoder[A] {
    final def apply(c: HCursor): Decoder.Result[A] = decode.value(c).map(gen.from)
    override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] =
      decode.value.decodeAccumulating(c).map(gen.from)
  }
}
