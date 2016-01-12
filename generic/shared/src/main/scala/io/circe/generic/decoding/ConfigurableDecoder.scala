package io.circe.generic.decoding

import cats.Apply
import cats.data.Xor
import io.circe.{ AccumulatingDecoder, ConfiguredDecoder, Decoder, DecodingFailure, HCursor }
import io.circe.generic.NotExported
import io.circe.generic.config._
import shapeless._, shapeless.labelled.{ FieldType, field }

trait DerivedDecoder[A] extends Decoder[A]

@export.exports
final object DerivedDecoder {
  implicit final def fromDerivedConfiguredDecoder[A](implicit
    decoder: DerivedConfiguredDecoder[Unit, A]
  ): DerivedDecoder[A] = decoder
}

trait DerivedConfiguredDecoder[C, A] extends ConfiguredDecoder[C, A] with DerivedDecoder[A]

@export.exports
final object DerivedConfiguredDecoder extends IncompleteDerivedDecoders {
  implicit final def deriveConfiguredDecoder[C, A](implicit
    decoder: Lazy[ConfigurableDecoder[C, A]],
    config: Configuration[C]
  ): DerivedConfiguredDecoder[C, A] = decoder.value(config)
}

trait ConfigurableDecoder[C, A] {
  def apply(config: Configuration[C]): DerivedConfiguredDecoder[C, A]
}

final object ConfigurableDecoder extends LowPriorityConfigurableDecoders {
  private[this] final def unconfigurable[C, A](decoder: DerivedConfiguredDecoder[C, A]): ConfigurableDecoder[C, A] =
    new ConfigurableDecoder[C, A] {
      final def apply(config: Configuration[C]): DerivedConfiguredDecoder[C, A] = decoder
    }

  final implicit def decodeHNil[C, A]: ConfigurableDecoder[C, HNil] = unconfigurable(
    new DerivedConfiguredDecoder[C, HNil] {
      final def apply(c: HCursor): Decoder.Result[HNil] = Xor.right(HNil)
    }
  )

  implicit final def decodeCNil[C, A]: ConfigurableDecoder[C, CNil] = unconfigurable(
    new DerivedConfiguredDecoder[C, CNil] {
      final def apply(c: HCursor): Decoder.Result[CNil] = Xor.left(DecodingFailure("CNil", c.history))
    }
  )

  implicit final def decodeLabelledHList[C, K <: Symbol, H, T <: HList](implicit
    name: Witness.Aux[K],
    headDecoder: NotExported[Decoder[H]],
    tailDecoder: Lazy[ConfigurableDecoder[C, T]]
  ): ConfigurableDecoder[C, FieldType[K, H] :: T] =
    new ConfigurableDecoder[C, FieldType[K, H] :: T] {
      final def apply(config: Configuration[C]): DerivedConfiguredDecoder[C, FieldType[K, H] :: T] =
        new HListDecoder[C, K, H, T](
          name.value.name,
          headDecoder.value,
          tailDecoder.value(config),
          config
        )
    }

  implicit final def decodeCoproduct[C, K <: Symbol, H, T <: Coproduct](implicit
    name: Witness.Aux[K],
    headDecoder: NotExported[Decoder[H]],
    tailDecoder: Lazy[ConfigurableDecoder[C, T]]
  ): ConfigurableDecoder[C, FieldType[K, H] :+: T] =
    new ConfigurableDecoder[C, FieldType[K, H] :+: T] {
      final def apply(config: Configuration[C]): DerivedConfiguredDecoder[C, FieldType[K, H] :+: T] =
        new CoproductDecoder[C, K, H, T](
          name.value.name,
          headDecoder.value,
          tailDecoder.value(config),
          config
        )
    }
}

private[circe] trait LowPriorityConfigurableDecoders {
  implicit final def decodeCoproductRec[C, K <: Symbol, H, T <: Coproduct](implicit
    name: Witness.Aux[K],
    headDecoder: Lazy[ConfigurableDecoder[C, H]],
    tailDecoder: Lazy[ConfigurableDecoder[C, T]]
  ): ConfigurableDecoder[C, FieldType[K, H] :+: T] =
    new ConfigurableDecoder[C, FieldType[K, H] :+: T] {
      final def apply(config: Configuration[C]): DerivedConfiguredDecoder[C, FieldType[K, H] :+: T] =
        new CoproductDecoder[C, K, H, T](
          name.value.name,
          headDecoder.value(config),
          tailDecoder.value(config),
          config
        )
    }

  implicit final def decodeLabelledHListRec[C, K <: Symbol, H, T <: HList](implicit
    name: Witness.Aux[K],
    headDecoder: Lazy[ConfigurableDecoder[C, H]],
    tailDecoder: Lazy[ConfigurableDecoder[C, T]]
  ): ConfigurableDecoder[C, FieldType[K, H] :: T] =
    new ConfigurableDecoder[C, FieldType[K, H] :: T] {
      final def apply(config: Configuration[C]): DerivedConfiguredDecoder[C, FieldType[K, H] :: T] =
        new HListDecoder[C, K, H, T](
          name.value.name,
          headDecoder.value(config),
          tailDecoder.value(config),
          config
        )
    }

  implicit final def decodeCaseClass[C, A, R <: HList](implicit
    gen: LabelledGeneric.Aux[A, R],
    decoder: Lazy[ConfigurableDecoder[C, R]]
  ): ConfigurableDecoder[C, A] = new ConfigurableDecoder[C, A] {
    final def apply(config: Configuration[C]): DerivedConfiguredDecoder[C, A] =
      new DerivedConfiguredDecoder[C, A] {
        final def apply(c: HCursor): Decoder.Result[A] = decoder.value(config)(c).map(gen.from)

        override final def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] =
          decoder.value(config).decodeAccumulating(c).map(gen.from)
      }
  }

  implicit final def decodeAdt[C, A, R <: Coproduct](implicit
    gen: LabelledGeneric.Aux[A, R],
    decoder: Lazy[ConfigurableDecoder[C, R]]
  ): ConfigurableDecoder[C, A] = new ConfigurableDecoder[C, A] {
    final def apply(config: Configuration[C]): DerivedConfiguredDecoder[C, A] =
      new DerivedConfiguredDecoder[C, A] {
        final def apply(c: HCursor): Decoder.Result[A] = decoder.value(config)(c).map(gen.from)

        override final def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] =
          decoder.value(config).decodeAccumulating(c).map(gen.from)
      }
  }
}

private[generic] class HListDecoder[C, K <: Symbol, H, T <: HList](
  name: String,
  headDecoder: Decoder[H],
  tailDecoder: Decoder[T],
  config: Configuration[C]
) extends DerivedConfiguredDecoder[C, FieldType[K, H] :: T] {
  private[this] val decoder: Decoder[FieldType[K, H] :: T] =
    Apply[Decoder].map2(
      headDecoder.prepare(_.downField(config.keyTransformation(name))),
      tailDecoder
    )((head, tail) => field[K](head) :: tail)

  final def apply(c: HCursor): Decoder.Result[FieldType[K, H] :: T] = decoder(c)

  override final def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[FieldType[K, H] :: T] =
    decoder.decodeAccumulating(c)
}

private[generic] class CoproductDecoder[C, K <: Symbol, H, T <: Coproduct](
  name: String,
  headDecoder: Decoder[H],
  tailDecoder: Decoder[T],
  config: Configuration[C]
) extends DerivedConfiguredDecoder[C, FieldType[K, H] :+: T] {
  private[this] def decodeObject(c: HCursor): Decoder.Result[FieldType[K, H] :+: T] =
    config.discriminator match {
      case ObjectWrapper =>
        c.downField(name).focus.fold[Xor[DecodingFailure, FieldType[K, H] :+: T]](
          tailDecoder(c).map(Inr(_))
        )(_.as(headDecoder).map(h => Inl(field(h))))
      case DiscriminatorKey(key) =>
        c.get[String](key).flatMap {
          case s if s == name => headDecoder(c).map(result => Inl(field(result)))
          case _ => Xor.left(DecodingFailure("Case class in ADT", c.history))
        }
    }

  final def apply(c: HCursor): Decoder.Result[FieldType[K, H] :+: T] =
    if (config.caseObjectEncoding == CaseObjectString) {
      c.as[String].flatMap {
        case s if s == name => headDecoder(c)
        case _ => Xor.left(DecodingFailure("Case object in ADT", c.history))
      }.fold(_ => decodeObject(c), result => Xor.right(Inl(field(result))))
    } else decodeObject(c)
}
