package io.circe.generic.decoding

import cats.data.Xor
import io.circe.{ Decoder, DecodingFailure, HCursor }
import shapeless._, shapeless.labelled.{ FieldType, field }

trait DerivedDecoder[A] extends Decoder[A]

@export.exports
object DerivedDecoder extends IncompleteDerivedDecoders with LowPriorityDerivedDecoders {
  implicit val decodeHNil: DerivedDecoder[HNil] =
    new DerivedDecoder[HNil] {
      def apply(c: HCursor): Decoder.Result[HNil] = Xor.right(HNil)
    }

  implicit val decodeCNil: DerivedDecoder[CNil] =
    new DerivedDecoder[CNil] {
      def apply(c: HCursor): Decoder.Result[CNil] = Xor.left(DecodingFailure("CNil", c.history))
    }

  implicit def decodeCoproduct[K <: Symbol, H, T <: Coproduct](implicit
    key: Witness.Aux[K],
    decodeHead: Lazy[Decoder[H]],
    decodeTail: Lazy[DerivedDecoder[T]]
  ): DerivedDecoder[FieldType[K, H] :+: T] = new DerivedDecoder[FieldType[K, H] :+: T] {
    def apply(c: HCursor): Decoder.Result[FieldType[K, H] :+: T] =
      c.downField(key.value.name).focus.fold[Xor[DecodingFailure, FieldType[K, H] :+: T]](
        decodeTail.value(c).map(Inr(_))
      ) { headJson =>
        headJson.as(decodeHead.value).map(h => Inl(field(h)))
      }
  }
  
  implicit def decodeLabelledHList[K <: Symbol, H, T <: HList](implicit
    key: Witness.Aux[K],
    decodeHead: Lazy[Decoder[H]],
    decodeTail: Lazy[DerivedDecoder[T]]
  ): DerivedDecoder[FieldType[K, H] :: T] = new DerivedDecoder[FieldType[K, H] :: T] 
    with Decoder.FromUnsafe[FieldType[K, H] :: T] {
      private[circe] override def decodeUnsafe(c: HCursor): FieldType[K, H] :: T =
        field[K](
          decodeHead.value.decodeUnsafe(
            c.downField(key.value.name).success.getOrElse(
              throw DecodingFailure("Attempt to decode value on failed cursor", c.history)
            )
          )
        ) :: decodeTail.value.decodeUnsafe(c)
    }
}

private[circe] trait LowPriorityDerivedDecoders {
  implicit def decodeCoproductDerived[K <: Symbol, H, T <: Coproduct](implicit
    key: Witness.Aux[K],
    decodeHead: Lazy[DerivedDecoder[H]],
    decodeTail: Lazy[DerivedDecoder[T]]
  ): DerivedDecoder[FieldType[K, H] :+: T] = new DerivedDecoder[FieldType[K, H] :+: T] {
    def apply(c: HCursor): Decoder.Result[FieldType[K, H] :+: T] =
      c.downField(key.value.name).focus.fold[Xor[DecodingFailure, FieldType[K, H] :+: T]](
        decodeTail.value(c).map(Inr(_))
      ) { headJson =>
        headJson.as(decodeHead.value).map(h => Inl(field(h)))
      }
  }

  implicit def decodeAdt[A, R <: Coproduct](implicit
    gen: LabelledGeneric.Aux[A, R],
    decode: Lazy[DerivedDecoder[R]]
  ): DerivedDecoder[A] = new DerivedDecoder[A] {
    def apply(c: HCursor): Decoder.Result[A] = decode.value(c).map(gen.from)
  }

  implicit def decodeCaseClass[A, R <: HList](implicit
    gen: LabelledGeneric.Aux[A, R],
    decode: Lazy[DerivedDecoder[R]]
  ): DerivedDecoder[A] = new DerivedDecoder[A] with Decoder.FromUnsafe[A] {
    private[circe] override def decodeUnsafe(c: HCursor): A = gen.from(decode.value.decodeUnsafe(c))
  }
}
