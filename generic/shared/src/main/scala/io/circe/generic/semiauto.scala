package io.circe.generic

import io.circe.{ Decoder, HCursor, JsonObject, ObjectEncoder }
import io.circe.generic.decoding.{
  DerivedDecoder,
  DerivedDecoderWithDefaults,
  DerivedDecoderWithDefaultsBuilder
}
import io.circe.generic.encoding.DerivedObjectEncoder
import io.circe.generic.util.PatchWithOptions
import shapeless.{ HList, Default, LabelledGeneric, Lazy }
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
 *     implicit val decodeFoo: Decoder[Foo] = deriveDecoder
 *     implicit val encodeFoo: Encoder[Foo] = deriveEncoder
 *   }
 * }}}
 */
object semiauto {
  def deriveDecoder[A](implicit decode: DerivedDecoder[A]): Decoder[A] = decode

  def deriveEncoder[A](implicit encode: DerivedObjectEncoder[A]): ObjectEncoder[A] = encode

  def deriveFor[A]: DerivationHelper[A] = new DerivationHelper[A]

  class DerivationHelper[A] {
    @deprecated("Use deriveDecoder", "0.3.0")
    def decoder[R](implicit
      gen: LabelledGeneric.Aux[A, R],
      decode: Lazy[DerivedDecoder[R]]
    ): Decoder[A] = new Decoder[A] {
      def apply(c: HCursor): Decoder.Result[A] = decode.value(c).map(gen.from)
    }

    @deprecated("Use deriveEncoder", "0.3.0")
    def encoder[R](implicit
      gen: LabelledGeneric.Aux[A, R],
      encode: Lazy[DerivedObjectEncoder[R]]
    ): ObjectEncoder[A] = new ObjectEncoder[A] {
      def encodeObject(a: A): JsonObject = encode.value.encodeObject(gen.to(a))
    }

    def decoderWithDefaults[R <: HList, D <: HList](implicit
      gen: LabelledGeneric.Aux[A, R],
      defaults: Default.AsRecord.Aux[A, D],
      builder: Lazy[DerivedDecoderWithDefaultsBuilder[R, D]]
    ): Decoder[A] = builder.value(defaults()).map(gen.from)

    def incomplete[P <: HList, C, T <: HList, R <: HList](implicit
      ffp: FnFromProduct.Aux[P => C, A],
      gen: LabelledGeneric.Aux[C, T],
      removeAll: RemoveAll.Aux[T, P, (P, R)],
      decode: DerivedDecoder[R]
    ): Decoder[A] = DerivedDecoder.decodeIncompleteCaseClass[A, P, C, T, R]

    def patch[R <: HList, O <: HList](implicit
      gen: LabelledGeneric.Aux[A, R],
      patch: PatchWithOptions.Aux[R, O],
      decode: DerivedDecoder[O]
    ): DerivedDecoder[A => A] = DerivedDecoder.decodeCaseClassPatch[A, R, O]
  }
}
