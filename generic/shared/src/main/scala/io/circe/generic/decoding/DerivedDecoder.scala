package io.circe.generic.decoding

import cats.data.Xor
import cats.syntax.monoidal._
import io.circe.{ AccumulatingDecoder, Decoder, DecodingFailure, HCursor }
import shapeless._, shapeless.labelled.{ FieldType, field }

trait DerivedDecoder[A] extends Decoder[A]

@export.exports
object DerivedDecoder extends IncompleteDerivedDecoders with LowPriorityDerivedDecoders {
  def fromDecoder[A](decode: Decoder[A]): DerivedDecoder[A] = new DerivedDecoder[A] {
    def apply(c: HCursor): Decoder.Result[A] = decode(c)
    override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] =
      decode.decodeAccumulating(c)
  }

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
  ): DerivedDecoder[FieldType[K, H] :: T] = fromDecoder(
    (decodeHead.value.prepare(_.downField(key.value.name)) |@| decodeTail.value).map(
      (head, tail) => field[K](head) :: tail
    )
  )
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
  ): DerivedDecoder[A] = new DerivedDecoder[A] {
    def apply(c: HCursor): Decoder.Result[A] = decode.value(c).map(gen.from)
    override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] =
      decode.value.decodeAccumulating(c).map(gen.from)
  }
}
