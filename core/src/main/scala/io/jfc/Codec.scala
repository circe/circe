package io.jfc

import cats.data.Xor
import cats.functor.Invariant

/**
 * A helper class that bundles together a [[Decoder]] and [[Encoder]].
 *
 * In jfc this type plays a slightly different role than it does in Argonaut: it's used primarily to
 * facilitate generic derivation. In general you should not require [[Codec]] instances, but instead
 * use [[Decoder]] or [[Encoder]] or both as appropriate.
 */
sealed abstract class Codec[A] { self =>
  val decoder: Decoder[A]
  val encoder: Encoder[A]

  def decode(c: HCursor): Xor[DecodingFailure, A] = decoder(c)
  def encode(a: A): Json = encoder(a)

  def withErrorMessage(message: String): Codec[A] =
    Codec.combined(self.decoder.withErrorMessage(message), self.encoder)

  def imap[B](f: A => B)(g: B => A): Codec[B] =
    Codec.combined(decoder.map(f), encoder.contramap(g))
}

object Codec {
  def apply[A](implicit c: Codec[A]): Codec[A] = c

  def instance[A](decode: HCursor => Xor[DecodingFailure, A], encode: A => Json): Codec[A] =
    combined(Decoder.instance(decode), Encoder.instance(encode))

  def withReattempt[A](decode: ACursor => Xor[DecodingFailure, A], encode: A => Json): Codec[A] =
    combined(Decoder.withReattempt(decode), Encoder.instance(encode))

  def combined[A](d: Decoder[A], e: Encoder[A]): Codec[A] =
    new Codec[A] {
      val decoder: Decoder[A] = d
      val encoder: Encoder[A] = e
    }

  implicit val invariantCodec: Invariant[Codec] = new Invariant[Codec] {
    def imap[A, B](fa: Codec[A])(f: A => B)(g: B => A): Codec[B] = fa.imap(f)(g)
  }
}
