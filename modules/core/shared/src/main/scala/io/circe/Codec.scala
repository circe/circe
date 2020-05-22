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
trait Codec[A, J] extends Decoder[A] with Encoder[A, J] {

  /**
   * Variant of `imap` which allows the [[Decoder]] to fail.
   * @param f decode value
   * @param g encode value
   * @tparam B the type of the new [[Codec]]
   * @return a codec for [[B]]
   */
  def iemap[B](f: A => Either[String, B])(g: B => A): Codec[B, J] = Codec.from(emap(f), contramap(g))

}

object Codec extends ProductCodecs with EnumerationCodecs {
  def apply[A, J](implicit instance: Codec[A, J]): Codec[A, J] = instance

  implicit def codecInvariant[J]: Invariant[Codec[?, J]] = new Invariant[Codec[?, J]] {
    override def imap[A, B](fa: Codec[A, J])(f: A => B)(g: B => A): Codec[B, J] = Codec.from(fa.map(f), fa.contramap(g))
  }

  final def codecForEither[A, B, J](leftKey: String, rightKey: String)(implicit
    decodeA: Decoder[A],
    encodeA: Encoder[A, J],
    decodeB: Decoder[B],
    encodeB: Encoder[B, J],
    J0: JsonFactory[J]
  ): AsObject[Either[A, B], J] = new AsObject[Either[A, B], J] {
    override protected def J: JsonFactory[J] = J0
    private[this] val decoder: Decoder[Either[A, B]] = Decoder.decodeEither(leftKey, rightKey)
    private[this] val encoder: Encoder.AsObject[Either[A, B], J] = Encoder.encodeEither(leftKey, rightKey)
    final def apply(c: HCursor): Decoder.Result[Either[A, B]] = decoder(c)
    final def encodeObject(a: Either[A, B]): JsonObject[J] = encoder.encodeObject(a)
  }

  final def codecForValidated[E, A, J](failureKey: String, successKey: String)(implicit
    decodeE: Decoder[E],
    encodeE: Encoder[E, J],
    decodeA: Decoder[A],
    encodeA: Encoder[A, J]
  ): AsObject[Validated[E, A], J] = new AsObject[Validated[E, A], J] {
    private[this] val decoder: Decoder[Validated[E, A]] = Decoder.decodeValidated(failureKey, successKey)
    private[this] val encoder: Encoder.AsObject[Validated[E, A], J] = Encoder.encodeValidated(failureKey, successKey)
    final def apply(c: HCursor): Decoder.Result[Validated[E, A]] = decoder(c)
    final def encodeObject(a: Validated[E, A]): JsonObject[J] = encoder.encodeObject(a)
  }

  def from[A, J](decodeA: Decoder[A], encodeA: Encoder[A, J]): Codec[A, J] =
    new Codec[A, J] {
      def apply(c: HCursor): Decoder.Result[A] = decodeA(c)
      def apply(a: A): J = encodeA(a)
    }

  trait AsRoot[A, J] extends Codec[A, J] with Encoder.AsRoot[A, J]

  object AsRoot {
    def apply[A, J](implicit instance: AsRoot[A, J]): AsRoot[A, J] = instance
  }

  trait AsArray[A, J] extends AsRoot[A, J] with Encoder.AsArray[A, J]

  object AsArray {
    def apply[A, J](implicit instance: AsArray[A, J]): AsArray[A, J] = instance

    def from[A, J](decodeA: Decoder[A], encodeA: Encoder.AsArray[A, J]): AsArray[A, J] =
      new AsArray[A, J] {
        def apply(c: HCursor): Decoder.Result[A] = decodeA(c)
        def encodeArray(a: A): Vector[J] = encodeA.encodeArray(a)
      }
  }

  trait AsObject[A, J] extends AsRoot[A, J] with Encoder.AsObject[A, J]

  object AsObject extends CodecDerivation {
    def apply[A, J](implicit instance: AsObject[A, J]): AsObject[A, J] = instance

    def from[A, J](decodeA: Decoder[A], encodeA: Encoder.AsObject[A, J]): AsObject[A, J] =
      new AsObject[A, J] {
        def apply(c: HCursor): Decoder.Result[A] = decodeA(c)
        def encodeObject(a: A): JsonObject[J] = encodeA.encodeObject(a)
      }
  }
}
