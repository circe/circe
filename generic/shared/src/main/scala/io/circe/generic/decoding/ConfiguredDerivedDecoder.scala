package io.circe.generic.decoding

import cats.data.Xor
import cats.syntax.monoidal._
import io.circe.{ AccumulatingDecoder, ConfiguredDecoder, Decoder, DecodingFailure, HCursor }
import io.circe.generic.config.{ SnakeCaseKeys, snakeCase }
import shapeless._, shapeless.labelled.{ FieldType, field }

trait ConfiguredDerivedDecoder[C, A] extends DerivedDecoder[A] with ConfiguredDecoder[C, A]

@export.exports
final object ConfiguredDerivedDecoder
  extends IncompleteDerivedDecoders with MidPriorityConfiguredDerivedDecoders {
  final implicit def decodeHNil[C]: ConfiguredDerivedDecoder[C, HNil] =
    new ConfiguredDerivedDecoder[C, HNil] {
      final def apply(c: HCursor): Decoder.Result[HNil] = Xor.right(HNil)
    }

  implicit final def decodeCNil[C]: ConfiguredDerivedDecoder[C, CNil] =
    new ConfiguredDerivedDecoder[C, CNil] {
      final def apply(c: HCursor): Decoder.Result[CNil] =
        Xor.left(DecodingFailure("CNil", c.history))
    }

  implicit final def decodeCoproduct[C, K <: Symbol, H, T <: Coproduct](implicit
    key: Witness.Aux[K],
    decodeHead: Lazy[ConfiguredDecoder[C, H]],
    decodeTail: Lazy[ConfiguredDerivedDecoder[C, T]]
  ): ConfiguredDerivedDecoder[C, FieldType[K, H] :+: T] =
    new ConfiguredDerivedDecoder[C, FieldType[K, H] :+: T] {
      final def apply(c: HCursor): Decoder.Result[FieldType[K, H] :+: T] =
        c.downField(key.value.name).focus.fold[Xor[DecodingFailure, FieldType[K, H] :+: T]](
          decodeTail.value(c).map(Inr(_))
        ) { headJson =>
          headJson.as(decodeHead.value).map(h => Inl(field(h)))
        }
    }

  implicit final def decodeLabelledHListSnakeCaseKeys[K <: Symbol, H, T <: HList](implicit
    key: Witness.Aux[K],
    decodeHead: Lazy[ConfiguredDecoder[SnakeCaseKeys, H]],
    decodeTail: Lazy[ConfiguredDerivedDecoder[SnakeCaseKeys, T]]
  ): ConfiguredDerivedDecoder[SnakeCaseKeys, FieldType[K, H] :: T] = fromDecoder(
    (decodeHead.value.prepare(_.downField(snakeCase(key.value.name))) |@| decodeTail.value).map(
      (head, tail) => field[K](head) :: tail
    )
  )
}

private[circe] trait MidPriorityConfiguredDerivedDecoders
  extends LowPriorityConfiguredDerivedDecoders {
  implicit final def decodeCoproductDerived[C, K <: Symbol, H, T <: Coproduct](implicit
    key: Witness.Aux[K],
    decodeHead: Lazy[ConfiguredDerivedDecoder[C, H]],
    decodeTail: Lazy[ConfiguredDerivedDecoder[C, T]]
  ): ConfiguredDerivedDecoder[C, FieldType[K, H] :+: T] =
    new ConfiguredDerivedDecoder[C, FieldType[K, H] :+: T] {
      final def apply(c: HCursor): Decoder.Result[FieldType[K, H] :+: T] =
        c.downField(key.value.name).focus.fold[Xor[DecodingFailure, FieldType[K, H] :+: T]](
          decodeTail.value(c).map(Inr(_))
        ) { headJson =>
          headJson.as(decodeHead.value).map(h => Inl(field(h)))
        }
    }

  implicit final def decodeLabelledHListSnakeCaseKeysBase[K <: Symbol, H, T <: HList](implicit
    key: Witness.Aux[K],
    decodeHead: Lazy[Decoder[H]],
    decodeTail: Lazy[ConfiguredDerivedDecoder[SnakeCaseKeys, T]]
  ): ConfiguredDerivedDecoder[SnakeCaseKeys, FieldType[K, H] :: T] = fromDecoder(
    (decodeHead.value.prepare(_.downField(snakeCase(key.value.name))) |@| decodeTail.value).map(
      (head, tail) => field[K](head) :: tail
    )
  )

  implicit final def decodeAdt[C, A, R <: Coproduct](implicit
    gen: LabelledGeneric.Aux[A, R],
    decode: Lazy[ConfiguredDerivedDecoder[C, R]]
  ): ConfiguredDerivedDecoder[C, A] = new ConfiguredDerivedDecoder[C, A] {
    final def apply(c: HCursor): Decoder.Result[A] = decode.value(c).map(gen.from)
  }

  implicit final def decodeCaseClass[C, A, R <: HList](implicit
    gen: LabelledGeneric.Aux[A, R],
    decode: Lazy[ConfiguredDerivedDecoder[C, R]]
  ): ConfiguredDerivedDecoder[C, A] = new ConfiguredDerivedDecoder[C, A] {
    final def apply(c: HCursor): Decoder.Result[A] = decode.value(c).map(gen.from)
    override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] =
      decode.value.decodeAccumulating(c).map(gen.from)
  }
}

private[circe] trait LowPriorityConfiguredDerivedDecoders {
  final def fromDecoder[C, A](decode: Decoder[A]): ConfiguredDerivedDecoder[C, A] =
    new ConfiguredDerivedDecoder[C, A] {
      final def apply(c: HCursor): Decoder.Result[A] = decode(c)
      override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] =
        decode.decodeAccumulating(c)
    }

  implicit final def decodeLabelledHListUnconfigured[C, K <: Symbol, H, T <: HList](implicit
    key: Witness.Aux[K],
    decodeHead: Lazy[Decoder[H]],
    decodeTail: Lazy[ConfiguredDerivedDecoder[C, T]]
  ): ConfiguredDerivedDecoder[C, FieldType[K, H] :: T] = fromDecoder(
    (decodeHead.value.prepare(_.downField(key.value.name)) |@| decodeTail.value).map(
      (head, tail) => field[K](head) :: tail
    )
  )
}
