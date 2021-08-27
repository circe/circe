package io.circe.generic.simple

import io.circe.{ Codec, Decoder, Encoder }
import io.circe.generic.simple.codec.DerivedAsObjectCodec
import io.circe.generic.simple.decoding.{ DerivedDecoder, ReprDecoder }
import io.circe.generic.simple.encoding.DerivedAsObjectEncoder
import io.circe.generic.simple.util.PatchWithOptions
import shapeless.{ HList, LabelledGeneric }
import shapeless.ops.function.FnFromProduct
import shapeless.ops.record.RemoveAll

/**
 * Semi-automatic codec derivation.
 *
 * This object provides helpers for creating [[io.circe.Decoder]] and [[io.circe.ObjectEncoder]]
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
 *     implicit val encodeFoo: ObjectEncoder[Foo] = deriveEncoder[Foo]
 *   }
 * }}}
 */
object semiauto {
  final def deriveDecoder[A](implicit decode: DerivedDecoder[A]): Decoder[A] = decode
  final def deriveEncoder[A](implicit encode: DerivedAsObjectEncoder[A]): Encoder.AsObject[A] = encode
  final def deriveCodec[A](implicit codec: DerivedAsObjectCodec[A]): Codec.AsObject[A] = codec

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
