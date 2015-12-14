package io.circe.generic.decoding

import cats.data.Xor
import io.circe.{ Decoder, DecodingFailure, HCursor }
import shapeless._, shapeless.labelled.{ FieldType, field }, shapeless.ops.record.Selector

trait DerivedDecoderWithDefaultsBuilder[A, D <: HList] {
  def apply(d: D): DerivedDecoderWithDefaults[A]
}

object DerivedDecoderWithDefaultsBuilder extends
  LowPriorityDerivedDecoderWithDefaultsBuilders {
    implicit def buildDecoderLabelledHList1[K <: Symbol, H, T <: HList, D <: HList](implicit
      key: Witness.Aux[K],
      decodeHead: Lazy[Decoder[H]],
      decodeTail: Lazy[DerivedDecoderWithDefaultsBuilder[T, D]],
      sel: Selector.Aux[D, K, H]
    ): DerivedDecoderWithDefaultsBuilder[FieldType[K, H] :: T, D] =
      new DerivedDecoderWithDefaultsBuilder[FieldType[K, H] :: T, D] {
        def apply(d: D): DerivedDecoderWithDefaults[FieldType[K, H] :: T] =
          new DerivedDecoderWithDefaults[FieldType[K, H] :: T] {
            def apply(c: HCursor): Decoder.Result[FieldType[K, H] :: T] =
              c.as(decodeTail.value(d)).map(
                field[K](c.get(key.value.name)(decodeHead.value).getOrElse(sel(d))) :: _
              )
          }
    }
  }

private[circe] trait LowPriorityDerivedDecoderWithDefaultsBuilders {
  implicit def buildDecoderLabelledHList0[K <: Symbol, H, T <: HList, D <: HList](implicit
    key: Witness.Aux[K],
    decodeHead: Lazy[Decoder[H]],
    decodeTail: Lazy[DerivedDecoderWithDefaultsBuilder[T, D]]
  ): DerivedDecoderWithDefaultsBuilder[FieldType[K, H] :: T, D] =
    new DerivedDecoderWithDefaultsBuilder[FieldType[K, H] :: T, D] {
      def apply(d: D): DerivedDecoderWithDefaults[FieldType[K, H] :: T] =
        new DerivedDecoderWithDefaults[FieldType[K, H] :: T] {
          def apply(c: HCursor): Decoder.Result[FieldType[K, H] :: T] =
            for {
              head <- c.get(key.value.name)(decodeHead.value)
              tail <- c.as(decodeTail.value(d))
            } yield field[K](head) :: tail
        }
  }

  implicit def fromDerivedDecoderWithDefaults[A, D <: HList](implicit
    decode: Lazy[DerivedDecoderWithDefaults[A]]
  ): DerivedDecoderWithDefaultsBuilder[A, D] = new DerivedDecoderWithDefaultsBuilder[A, D] {
    def apply(defs: D): DerivedDecoderWithDefaults[A] = decode.value
  }
}

trait DerivedDecoderWithDefaults[A] extends DerivedDecoder[A]

@export.exports
object DerivedDecoderWithDefaults extends MidPriorityDerivedDecodersWithDefaults {
  implicit def decodeCaseClassWithDefaults[A, R <: HList, D <: HList](implicit
    gen: LabelledGeneric.Aux[A, R],
    defaults: Default.AsRecord.Aux[A, D],
    builder: Lazy[DerivedDecoderWithDefaultsBuilder[R, D]]
  ): DerivedDecoderWithDefaults[A] = new DerivedDecoderWithDefaults[A] {
    private[this] val decoder = builder.value(defaults()).map(gen.from)

    def apply(c: HCursor): Decoder.Result[A] = decoder(c)
  }
}

private[circe] trait MidPriorityDerivedDecodersWithDefaults
  extends LowPriorityDerivedDecodersWithDefaults {
    implicit def lowerDerivedDecoder[A](implicit
      decode: DerivedDecoder[A]
    ): DerivedDecoderWithDefaults[A] = new DerivedDecoderWithDefaults[A] {
      def apply(c: HCursor): Decoder.Result[A] = decode(c)
    }
  }

private[circe] trait LowPriorityDerivedDecodersWithDefaults {
  implicit def lowerDecoder[A](implicit
    decode: Decoder[A]
  ): DerivedDecoderWithDefaults[A] = new DerivedDecoderWithDefaults[A] {
    def apply(c: HCursor): Decoder.Result[A] = decode(c)
  }
}
