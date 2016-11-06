package io.circe.generic.extras

import io.circe.{ Decoder, Encoder, ObjectEncoder }
import io.circe.generic.extras.decoding.{ ConfiguredDecoder, EnumerationDecoder, ReprDecoder }
import io.circe.generic.extras.encoding.{ ConfiguredObjectEncoder, EnumerationEncoder }
import io.circe.generic.extras.util.RecordToMap
import io.circe.generic.util.PatchWithOptions
import shapeless.{ Default, HList, LabelledGeneric, Lazy }
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
final object semiauto {
  final def deriveDecoder[A](implicit decode: Lazy[ConfiguredDecoder[A]]): Decoder[A] = decode.value
  final def deriveEncoder[A](implicit encode: Lazy[ConfiguredObjectEncoder[A]]): ObjectEncoder[A] = encode.value

  final def deriveFor[A]: DerivationHelper[A] = new DerivationHelper[A]

  /**
   * Derive a decoder for a sealed trait hierarchy made up of case objects.
   *
   * Note that this differs from the usual derived decoder in that the leaves of
   * the ADT are represented as JSON strings.
   */
  def deriveEnumerationDecoder[A](implicit decode: Lazy[EnumerationDecoder[A]]): Decoder[A] = decode.value

  /**
   * Derive an encoder for a sealed trait hierarchy made up of case objects.
   *
   * Note that this differs from the usual derived encoder in that the leaves of
   * the ADT are represented as JSON strings.
   */
  def deriveEnumerationEncoder[A](implicit encode: Lazy[EnumerationEncoder[A]]): Encoder[A] = encode.value

  final class DerivationHelper[A] {
    final def incomplete[P <: HList, C, D <: HList, T <: HList, R <: HList](implicit
      ffp: FnFromProduct.Aux[P => C, A],
      gen: LabelledGeneric.Aux[C, T],
      removeAll: RemoveAll.Aux[T, P, (P, R)],
      decode: ReprDecoder[R],
      defaults: Default.AsRecord.Aux[C, D],
      defaultMapper: RecordToMap[D],
      config: Configuration
    ): Decoder[A] = ConfiguredDecoder.decodeIncompleteCaseClass[A, P, C, D, T, R]

    final def patch[D <: HList, R <: HList, O <: HList](implicit
      gen: LabelledGeneric.Aux[A, R],
      patch: PatchWithOptions.Aux[R, O],
      decode: ReprDecoder[O],
      defaults: Default.AsRecord.Aux[A, D],
      defaultMapper: RecordToMap[D],
      config: Configuration
    ): Decoder[A => A] = ConfiguredDecoder.decodeCaseClassPatch[A, D, R, O]
  }
}
