package io.circe.generic.decoding

import cats.Apply
import cats.data.{ Validated, Xor }
import io.circe.{ AccumulatingDecoder, ConfiguredDecoder, Decoder, DecodingFailure, HCursor }
import io.circe.generic.NotExported
import io.circe.generic.config._
import shapeless._, shapeless.labelled.{ FieldType, field }, shapeless.ops.record.Selector

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
    decoder: Lazy[ConfigurableDecoder[C, A, HNil]],
    config: Configuration[C]
  ): DerivedConfiguredDecoder[C, A] = decoder.value(config, HNil)
}

trait ConfigurableDecoder[C, A, D <: HList] {
  def apply(config: Configuration[C], defaults: D): DerivedConfiguredDecoder[C, A]
}

final object ConfigurableDecoder extends LowPriorityConfigurableDecoders {
  private[this] final def unconfigurable[C, A, D <: HList](
    decoder: DerivedConfiguredDecoder[C, A]
  ): ConfigurableDecoder[C, A, D] =
    new ConfigurableDecoder[C, A, D] {
      final def apply(config: Configuration[C], defaults: D): DerivedConfiguredDecoder[C, A] = decoder
    }

  final implicit def decodeHNil[C, A, D <: HList]: ConfigurableDecoder[C, HNil, D] = unconfigurable(
    new DerivedConfiguredDecoder[C, HNil] {
      final def apply(c: HCursor): Decoder.Result[HNil] = Xor.right(HNil)
    }
  )

  implicit final def decodeCNil[C, A]: ConfigurableDecoder[C, CNil, HNil] = unconfigurable(
    new DerivedConfiguredDecoder[C, CNil] {
      final def apply(c: HCursor): Decoder.Result[CNil] = Xor.left(DecodingFailure("CNil", c.history))
    }
  )

  implicit final def decodeLabelledHList[C, K <: Symbol, H, T <: HList, D <: HList](implicit
    name: Witness.Aux[K],
    headDecoder: NotExported[Decoder[H]],
    tailDecoder: Lazy[ConfigurableDecoder[C, T, D]],
    selector: Selector.Aux[D, K, H] = null
  ): ConfigurableDecoder[C, FieldType[K, H] :: T, D] =
    new ConfigurableDecoder[C, FieldType[K, H] :: T, D] {
      final def apply(config: Configuration[C], defaults: D): DerivedConfiguredDecoder[C, FieldType[K, H] :: T] =
        new HListDecoder[C, K, H, T](
          name.value.name,
          headDecoder.value,
          tailDecoder.value(config, defaults),
          config,
          Option(selector).map(_(defaults))
        )
    }

  implicit final def decodeCoproduct[C, K <: Symbol, H, T <: Coproduct](implicit
    name: Witness.Aux[K],
    headDecoder: NotExported[Decoder[H]],
    tailDecoder: Lazy[ConfigurableDecoder[C, T, HNil]]
  ): ConfigurableDecoder[C, FieldType[K, H] :+: T, HNil] =
    new ConfigurableDecoder[C, FieldType[K, H] :+: T, HNil] {
      final def apply(config: Configuration[C], defaults: HNil): DerivedConfiguredDecoder[C, FieldType[K, H] :+: T] =
        new CoproductDecoder[C, K, H, T](
          name.value.name,
          headDecoder.value,
          tailDecoder.value(config, HNil),
          config
        )
    }
}

private[circe] trait LowPriorityConfigurableDecoders {
  implicit final def decodeCoproductRec[C, K <: Symbol, H, T <: Coproduct](implicit
    name: Witness.Aux[K],
    headDecoder: Lazy[ConfigurableDecoder[C, H, HNil]],
    tailDecoder: Lazy[ConfigurableDecoder[C, T, HNil]]
  ): ConfigurableDecoder[C, FieldType[K, H] :+: T, HNil] =
    new ConfigurableDecoder[C, FieldType[K, H] :+: T, HNil] {
      final def apply(config: Configuration[C], defaults: HNil): DerivedConfiguredDecoder[C, FieldType[K, H] :+: T] =
        new CoproductDecoder[C, K, H, T](
          name.value.name,
          headDecoder.value(config, HNil),
          tailDecoder.value(config, HNil),
          config
        )
    }

  implicit final def decodeLabelledHListRec[C, K <: Symbol, H, T <: HList, D <: HList](implicit
    name: Witness.Aux[K],
    headDecoder: Lazy[ConfigurableDecoder[C, H, HNil]],
    tailDecoder: Lazy[ConfigurableDecoder[C, T, D]],
    selector: Selector.Aux[D, K, H] = null
  ): ConfigurableDecoder[C, FieldType[K, H] :: T, D] =
    new ConfigurableDecoder[C, FieldType[K, H] :: T, D] {
      final def apply(config: Configuration[C], defaults: D): DerivedConfiguredDecoder[C, FieldType[K, H] :: T] =
        new HListDecoder[C, K, H, T](
          name.value.name,
          headDecoder.value(config, HNil),
          tailDecoder.value(config, defaults),
          config,
          Option(selector).map(_(defaults))
        )
    }

  implicit final def decodeCaseClass[C, A, R <: HList, D <: HList](implicit
    gen: LabelledGeneric.Aux[A, R],
    caseClassDefaults: Default.AsRecord.Aux[A, D],
    decoder: Lazy[ConfigurableDecoder[C, R, D]]
  ): ConfigurableDecoder[C, A, HNil] = new ConfigurableDecoder[C, A, HNil] {
    final def apply(config: Configuration[C], defaults: HNil): DerivedConfiguredDecoder[C, A] =
      new DerivedConfiguredDecoder[C, A] {
        final def apply(c: HCursor): Decoder.Result[A] = decoder.value(config, caseClassDefaults())(c).map(gen.from)

        override final def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] =
          decoder.value(config, caseClassDefaults()).decodeAccumulating(c).map(gen.from)
      }
  }

  implicit final def decodeAdt[C, A, R <: Coproduct](implicit
    gen: LabelledGeneric.Aux[A, R],
    decoder: Lazy[ConfigurableDecoder[C, R, HNil]]
  ): ConfigurableDecoder[C, A, HNil] = new ConfigurableDecoder[C, A, HNil] {
    final def apply(config: Configuration[C], defaults: HNil): DerivedConfiguredDecoder[C, A] =
      new DerivedConfiguredDecoder[C, A] {
        final def apply(c: HCursor): Decoder.Result[A] = decoder.value(config, HNil)(c).map(gen.from)

        override final def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] =
          decoder.value(config, HNil).decodeAccumulating(c).map(gen.from)
      }
  }
}

private[generic] class HListDecoder[C, K <: Symbol, H, T <: HList](
  name: String,
  headDecoder: Decoder[H],
  tailDecoder: Decoder[T],
  config: Configuration[C],
  default: Option[H]
) extends DerivedConfiguredDecoder[C, FieldType[K, H] :: T] {
  private[this] def withDefaultValues(preparedDecoder: Decoder[H]): Decoder[H] =
    if (config.defaultValues == NoDefaultValues) preparedDecoder else new Decoder[H] {
      final def apply(c: HCursor): Decoder.Result[H] = preparedDecoder(c) match {
        case result @ Xor.Right(_) => result
        case Xor.Left(error) => Xor.fromOption(default, error)
      }

      override final def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[H] =
        preparedDecoder.decodeAccumulating(c) match {
          case result @ Validated.Valid(_) => result
          case Validated.Invalid(errors) => Validated.fromOption(default, errors)
        }
    }

  private[this] val decoder: Decoder[FieldType[K, H] :: T] =
    Apply[Decoder].map2(
      withDefaultValues(headDecoder.prepare(_.downField(config.keyTransformation(name)))),
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
