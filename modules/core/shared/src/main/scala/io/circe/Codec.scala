package io.circe

import cats.Invariant
import cats.data.Validated

/**
 * A type class that provides back and forth conversion between values of type `A`
 * and the [[Json]] format.
 *
 * Note that this type class is only intended to make instance definition more
 * convenient; it generally should not be used as a constraint.
 *
 * Instances should obey the laws defined in [[io.circe.testing.CodecLaws]].
 */
trait Codec[A] extends Decoder[A] with Encoder[A] {

  /**
   * Variant of `imap` which allows the [[Decoder]] to fail.
   * @param f decode value
   * @param g encode value
   * @tparam B the type of the new [[Codec]]
   * @return a codec for [[B]]
   */
  def iemap[B](f: A => Either[String, B])(g: B => A): Codec[B] = Codec.from(emap(f), contramap(g))

}

object Codec extends ProductCodecs with EnumerationCodecs {
  def apply[A](implicit decoder: Decoder[A], encoder: Encoder[A]): Codec[A] =
    decoder match {
      case codec: Codec[_] if codec eq encoder =>
        // if codec: Decoder[A] with Decoder[B], A and B must be identical
        codec.asInstanceOf[Codec[A]]
      case _ => from(decoder, encoder)
    }

  implicit val codecInvariant: Invariant[Codec] = new Invariant[Codec] {
    override def imap[A, B](fa: Codec[A])(f: A => B)(g: B => A): Codec[B] = Codec.from(fa.map(f), fa.contramap(g))
  }

  final def codecForEither[A, B](leftKey: String, rightKey: String)(implicit
    decodeA: Decoder[A],
    encodeA: Encoder[A],
    decodeB: Decoder[B],
    encodeB: Encoder[B]
  ): AsObject[Either[A, B]] = new AsObject[Either[A, B]] {
    private[this] val decoder: Decoder[Either[A, B]] = Decoder.decodeEither(leftKey, rightKey)
    private[this] val encoder: Encoder.AsObject[Either[A, B]] = Encoder.encodeEither(leftKey, rightKey)
    final def apply(c: HCursor): Decoder.Result[Either[A, B]] = decoder(c)
    final def encodeObject(a: Either[A, B]): JsonObject = encoder.encodeObject(a)
  }

  final def codecForValidated[E, A](failureKey: String, successKey: String)(implicit
    decodeE: Decoder[E],
    encodeE: Encoder[E],
    decodeA: Decoder[A],
    encodeA: Encoder[A]
  ): AsObject[Validated[E, A]] = new AsObject[Validated[E, A]] {
    private[this] val decoder: Decoder[Validated[E, A]] = Decoder.decodeValidated(failureKey, successKey)
    private[this] val encoder: Encoder.AsObject[Validated[E, A]] = Encoder.encodeValidated(failureKey, successKey)
    final def apply(c: HCursor): Decoder.Result[Validated[E, A]] = decoder(c)
    final def encodeObject(a: Validated[E, A]): JsonObject = encoder.encodeObject(a)
  }

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

  object AsObject extends CodecDerivation {
    def apply[A](implicit instance: AsObject[A]): AsObject[A] = instance

    def from[A](decodeA: Decoder[A], encodeA: Encoder.AsObject[A]): AsObject[A] =
      new AsObject[A] {
        def apply(c: HCursor): Decoder.Result[A] = decodeA(c)
        def encodeObject(a: A): JsonObject = encodeA.encodeObject(a)
      }
  }
}
