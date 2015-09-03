package io.circe.generic.decoding

import cats.data.Xor
import io.circe.{ Decoder, DecodingFailure, HCursor }
import shapeless._, shapeless.labelled.{ FieldType, field }

trait DerivedDecoder[A] extends Decoder[A]

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
    decodeHead: Strict[Priority[Decoder[H], DerivedDecoder[H]]],
    decodeTail: Lazy[DerivedDecoder[T]]
  ): DerivedDecoder[FieldType[K, H] :+: T] = new DerivedDecoder[FieldType[K, H] :+: T] {
    def apply(c: HCursor): Decoder.Result[FieldType[K, H] :+: T] =
      c.downField(key.value.name).focus.fold[Xor[DecodingFailure, FieldType[K, H] :+: T]](
        decodeTail.value(c).map(Inr(_))
      ) { headJson =>
        headJson.as(decodeHead.value.fold(identity)(identity)).map(h => Inl(field(h)))
      }
  }
  
  implicit def decodeLabelledHList0[K <: Symbol, H, T <: HList](implicit
    key: Witness.Aux[K],
    decodeHead: Strict[Priority[Decoder[H], DerivedDecoder[H]]],
    decodeTail: Lazy[DerivedDecoder[T]]
  ): DerivedDecoder[FieldType[K, H] :: T] = new DerivedDecoder[FieldType[K, H] :: T] {
    def apply(c: HCursor): Decoder.Result[FieldType[K, H] :: T] =
      for {
        head <- c.get(key.value.name)(decodeHead.value.fold(identity)(identity))
        tail <- c.as(decodeTail.value)
      } yield field[K](head) :: tail
  }
}

trait LowPriorityDerivedDecoders {
  implicit def decodeAdt[A, R <: Coproduct](implicit
    gen: LabelledGeneric.Aux[A, R],
    decode: Lazy[DerivedDecoder[R]]
  ): DerivedDecoder[A] = new DerivedDecoder[A] {
    def apply(c: HCursor): Decoder.Result[A] = decode.value(c).map(gen.from)
  }

  implicit def decodeCaseClass[A, R <: HList](implicit
    gen: LabelledGeneric.Aux[A, R],
    decode: Lazy[DerivedDecoder[R]]
  ): DerivedDecoder[A] = new DerivedDecoder[A] {
    def apply(c: HCursor): Decoder.Result[A] = decode.value(c).map(gen.from)
  }
}
