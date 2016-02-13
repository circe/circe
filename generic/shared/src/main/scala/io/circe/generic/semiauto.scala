package io.circe.generic

import io.circe.{ Decoder, HCursor, JsonObject, ObjectEncoder }
import io.circe.generic.decoding.DerivedDecoder
import io.circe.generic.encoding.DerivedObjectEncoder
import io.circe.generic.util.{ Complement, PatchWithOptions }
import shapeless.{ HList, LabelledGeneric, Lazy }, shapeless.ops.function.FnFromProduct

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
  final def deriveDecoder[A](implicit decode: Lazy[DerivedDecoder[A]]): Decoder[A] = decode.value

  final def deriveEncoder[A](implicit encode: Lazy[DerivedObjectEncoder[A]]): ObjectEncoder[A] =
    encode.value

  final def deriveFor[A]: DerivationHelper[A] = new DerivationHelper[A]

  final class DerivationHelper[A] {
    @deprecated("Use deriveDecoder", "0.3.0")
    final def decoder[R](implicit
      gen: LabelledGeneric.Aux[A, R],
      decode: Lazy[DerivedDecoder[R]]
    ): Decoder[A] = new Decoder[A] {
      final def apply(c: HCursor): Decoder.Result[A] = decode.value(c).map(gen.from)
    }

    @deprecated("Use deriveEncoder", "0.3.0")
    final def encoder[R](implicit
      gen: LabelledGeneric.Aux[A, R],
      encode: Lazy[DerivedObjectEncoder[R]]
    ): ObjectEncoder[A] = new ObjectEncoder[A] {
      final def encodeObject(a: A): JsonObject = encode.value.encodeObject(gen.to(a))
    }

    final def incomplete[P <: HList, C, T <: HList, R <: HList](implicit
      ffp: FnFromProduct.Aux[P => C, A],
      gen: LabelledGeneric.Aux[C, T],
      complement: Complement.Aux[T, P, R],
      decode: DerivedDecoder[R]
    ): Decoder[A] = DerivedDecoder.decodeIncompleteCaseClass[A, P, C, T, R]

    final def patch[R <: HList, O <: HList](implicit
      gen: LabelledGeneric.Aux[A, R],
      patch: PatchWithOptions.Aux[R, O],
      decode: DerivedDecoder[O]
    ): DerivedDecoder[A => A] = DerivedDecoder.decodeCaseClassPatch[A, R, O]
  }
}
