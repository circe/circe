/*
 * Copyright 2024 circe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.circe

import cats.Invariant
import cats.data.Validated

import scala.util.Try

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
   * Variant of `imap` which allows the [[Decoder]] to fail with message string.
   * @param f decode value or fail
   * @param g encode value
   * @tparam B the type of the new [[Codec]]
   * @return a codec for [[B]]
   */
  def iemap[B](f: A => Either[String, B])(g: B => A): Codec[B] = Codec.from(emap(f), contramap(g))

  /**
   * Variant of `imap` which allows the [[Decoder]] to fail with Throwable.
   * @param f decode value or fail
   * @param g encode value
   * @tparam B the type of the new [[Codec]]
   * @return a codec for [[B]]
   */
  def iemapTry[B](f: A => Try[B])(g: B => A): Codec[B] = Codec.from(emapTry(f), contramap(g))

  final override def at(field: String): Codec[A] = Codec.from(super[Decoder].at(field), super[Encoder].at(field))
}

object Codec extends ProductCodecs with ProductTypedCodecs with EnumerationCodecs with CodecDerivationRelaxed {
  def apply[A](implicit instance: Codec[A]): Codec[A] = instance

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
      override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[A] = decodeA.decodeAccumulating(c)
      override def tryDecode(c: ACursor): Decoder.Result[A] = decodeA.tryDecode(c)
      override def tryDecodeAccumulating(c: ACursor): Decoder.AccumulatingResult[A] = decodeA.tryDecodeAccumulating(c)

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
