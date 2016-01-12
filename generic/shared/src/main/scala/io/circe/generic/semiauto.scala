package io.circe.generic

import io.circe.{ ConfiguredDecoder, ConfiguredEncoder, Decoder, Encoder, HCursor, Json }
import io.circe.generic.decoding.{ DerivedConfiguredDecoder, DerivedDecoder }
import io.circe.generic.encoding.{ DerivedConfiguredEncoder, DerivedEncoder }
import io.circe.generic.util.PatchWithOptions
import shapeless.{ HList, LabelledGeneric, Lazy }
import shapeless.ops.function.FnFromProduct
import shapeless.ops.record.RemoveAll

/**
 * Semi-automatic codec derivation.
 *
 * This object provides helpers for creating [[io.circe.Decoder]] and [[io.circe.Encoder]] instances
 * for case classes, "incomplete" case classes, sealed trait hierarchies, etc.
 *
 * Typical usage will look like the following:
 *
 * {{{
 *   import io.circe._, io.circe.generic.semiauto._
 *
 *   case class Foo(i: Int, p: (String, Double))
 *
 *   object Foo {
 *     implicit val decodeFoo: Decoder[Foo] = deriveDecoder[Foo]
 *     implicit val encodeFoo: Encoder[Foo] = deriveEncoder[Foo]
 *   }
 * }}}
 */
final object semiauto {
  final def deriveDecoder[A](implicit decoder: Lazy[DerivedDecoder[A]]): Decoder[A] = decoder.value
  final def deriveEncoder[A](implicit encoder: Lazy[DerivedEncoder[A]]): Encoder[A] = encoder.value

  final def deriveConfiguredDecoder[A, C](implicit
    decoder: Lazy[DerivedConfiguredDecoder[C, A]]
  ): ConfiguredDecoder[C, A] = decoder.value

  final def deriveConfiguredEncoder[A, C](implicit
    encoder: Lazy[DerivedConfiguredEncoder[C, A]]
  ): ConfiguredEncoder[C, A] = encoder.value

  final def deriveFor[A]: DerivationHelper[A, Unit, Decoder] = new DerivationHelper[A, Unit, Decoder]
  final def deriveConfiguredFor[A, C]: DerivationHelper[A, C, ({ type L[x] = ConfiguredDecoder[C, x] })#L] =
    new DerivationHelper[A, C, ({ type L[x] = ConfiguredDecoder[C, x] })#L]

  final class DerivationHelper[A, C, Res[x] >: ConfiguredDecoder[C, x]] {
    final def incomplete[P <: HList, B, T <: HList, R <: HList](implicit
      ffp: FnFromProduct.Aux[P => B, A],
      gen: LabelledGeneric.Aux[B, T],
      removeAll: RemoveAll.Aux[T, P, (P, R)],
      decoder: DerivedConfiguredDecoder[C, R]
    ): Res[A] =
      DerivedConfiguredDecoder.decodeIncompleteCaseClass[C, A, P, B, T, R]

    final def patch[R <: HList, O <: HList](implicit
      gen: LabelledGeneric.Aux[A, R],
      patch: PatchWithOptions.Aux[R, O],
      decoder: DerivedConfiguredDecoder[C, O]
    ): Res[A => A] =
      DerivedConfiguredDecoder.decodeCaseClassPatch[C, A, R, O]
  }
}
