package io.circe.generic

import io.circe.{ Codec, Decoder, Encoder }
import io.circe.generic.codec.DerivedAsObjectCodec
import io.circe.generic.decoding.{ DerivedDecoder, ReprDecoder }
import io.circe.generic.encoding.DerivedAsObjectEncoder
import io.circe.generic.util.PatchWithOptions
import shapeless.{ HList, LabelledGeneric, Lazy }
import shapeless.ops.function.FnFromProduct
import shapeless.ops.record.RemoveAll

/**
 * Semi-automatic codec derivation.
 *
 * This object provides helpers for creating [[io.circe.Decoder]] and [[io.circe.Encoder.AsObject]]
 * instances for case classes, "incomplete" case classes, sealed trait hierarchies, etc.
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
 *     implicit val encodeFoo: Encoder.AsObject[Foo] = deriveEncoder[Foo]
 *   }
 * }}}
 */
object semiauto {
  final def deriveDecoder[A](implicit decode: Lazy[DerivedDecoder[A]]): Decoder[A] = decode.value
  final def deriveEncoder[A](implicit encode: Lazy[DerivedAsObjectEncoder[A]]): Encoder.AsObject[A] = encode.value
  final def deriveCodec[A](implicit codec: Lazy[DerivedAsObjectCodec[A]]): Codec.AsObject[A] = codec.value

  final def deriveFor[A]: DerivationHelper[A] = new DerivationHelper[A]

  final class DerivationHelper[A] {
    final def incomplete[P <: HList, C, T <: HList, R <: HList](implicit
      ffp: FnFromProduct.Aux[P => C, A],
      gen: LabelledGeneric.Aux[C, T],
      removeAll: RemoveAll.Aux[T, P, (P, R)],
      decode: ReprDecoder[R]
    ): Decoder[A] = DerivedDecoder.decodeIncompleteCaseClass[A, P, C, T, R]

    final def patch[R <: HList, O <: HList](implicit
      gen: LabelledGeneric.Aux[A, R],
      patch: PatchWithOptions.Aux[R, O],
      decode: ReprDecoder[O]
    ): Decoder[A => A] = DerivedDecoder.decodeCaseClassPatch[A, R, O]
  }
}
