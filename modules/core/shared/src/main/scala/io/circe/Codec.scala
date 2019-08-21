package io.circe

import cats.data.Validated
import scala.util.{ Failure, Success, Try }

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

  /**
   * {{{
   *   object WeekDay extends Enumeration { ... }
   *   implicit val weekDayCodec = Codec.codecForEnumeration(WeekDay)
   * }}}
   * @group Utilities
   */
  final def codecForEnumeration[E <: Enumeration](enum: E): Codec[E#Value] = new Codec[E#Value] {
    final def apply(c: HCursor): Decoder.Result[E#Value] = Decoder.decodeString(c).flatMap { str =>
      Try(enum.withName(str)) match {
        case Success(a) => Right(a)
        case Failure(t) => Left(DecodingFailure(t.getMessage, c.history))
      }
    }
    final def apply(e: E#Value): Json = Encoder.encodeString(e.toString)
  }

  final def codecForEither[A, B](leftKey: String, rightKey: String)(
    implicit
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

  final def codecForValidated[E, A](failureKey: String, successKey: String)(
    implicit
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

  object AsObject {
    def apply[A](implicit instance: AsObject[A]): AsObject[A] = instance

    def from[A](decodeA: Decoder[A], encodeA: Encoder.AsObject[A]): AsObject[A] =
      new AsObject[A] {
        def apply(c: HCursor): Decoder.Result[A] = decodeA(c)
        def encodeObject(a: A): JsonObject = encodeA.encodeObject(a)
      }
  }
}
