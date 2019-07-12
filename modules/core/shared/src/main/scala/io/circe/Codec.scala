package io.circe

/**
 * A type class that provides back and forth conversion between values of type `A`
 * and the [[Json]] format.
 *
 * Note that this type class is only intended to make instance definition more
 * convenient; it generally should not be used as a constraint.
 *
 * Instances should obey the laws defined in [[io.circe.testing.CodecLaws]].
 */
trait Codec[A] extends Decoder[A] with Encoder[A]

object Codec extends ProductCodecs {
  def apply[A](implicit instance: Codec[A]): Codec[A] = instance

  def from[A](decodeA: Decoder[A], encodeA: Encoder[A]): Codec[A] =
    new Codec[A] {
      def apply(c: HCursor): Decoder.Result[A] = decodeA(c)
      def apply(a: A): Json = encodeA(a)
    }

  trait AsRoot[A] extends Codec[A] with Encoder.AsRoot[A]

  object AsRoot {
    def apply[A](implicit instance: AsRoot[A]): AsRoot[A] = instance
  }

  trait AsArray[A] extends AsRoot[A] with Encoder.AsArray[A]

  object AsArray {
    def apply[A](implicit instance: AsArray[A]): AsArray[A] = instance

    def from[A](decodeA: Decoder[A], encodeA: Encoder.AsArray[A]): AsArray[A] =
      new AsArray[A] {
        def apply(c: HCursor): Decoder.Result[A] = decodeA(c)
        def encodeArray(a: A): Vector[Json] = encodeA.encodeArray(a)
      }
  }

  trait AsObject[A] extends AsRoot[A] with Encoder.AsObject[A]

  object AsObject {
    def apply[A](implicit instance: AsObject[A]): AsObject[A] = instance

    def from[A](decodeA: Decoder[A], encodeA: Encoder.AsObject[A]): AsObject[A] =
      new AsObject[A] {
        def apply(c: HCursor): Decoder.Result[A] = decodeA(c)
        def encodeObject(a: A): JsonObject = encodeA.encodeObject(a)
      }
  }
}
