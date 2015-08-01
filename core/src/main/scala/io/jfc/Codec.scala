package io.jfc

import cats.data.Xor
import cats.functor.Invariant

/**
 * A helper class that bundles together a [[Decode]] and [[Encode]].
 *
 * In jfc this type plays a slightly different role than it does in Argonaut:
 * it's used primarily to facilitate generic derivation. In general you should
 * not require [[Codec]] instances, but instead use [[Decode]] or [[Encode]] or
 * both as appropriate.
 */
sealed abstract class Codec[A] { self =>
  val decoder: Decode[A]
  val encoder: Encode[A]

  def decode(c: HCursor): Xor[DecodeFailure, A] = decoder(c)
  def encode(a: A): Json = encoder(a)

  def withErrorMessage(message: String): Codec[A] =
    Codec.combined(self.decoder.withErrorMessage(message), self.encoder)

  def imap[B](f: A => B)(g: B => A): Codec[B] =
    Codec.combined(decoder.map(f), encoder.contramap(g))
}

object Codec {
  def apply[A](implicit c: Codec[A]): Codec[A] = c

  def instance[A](decode: HCursor => Xor[DecodeFailure, A], encode: A => Json): Codec[A] =
    combined(Decode.instance(decode), Encode.instance(encode))

  def withReattempt[A](decode: ACursor => Xor[DecodeFailure, A], encode: A => Json): Codec[A] =
    combined(Decode.withReattempt(decode), Encode.instance(encode))

  def combined[A](d: Decode[A], e: Encode[A]): Codec[A] =
    new Codec[A] {
      val decoder: Decode[A] = d
      val encoder: Encode[A] = e
    }

  implicit val invariantCodec: Invariant[Codec] = new Invariant[Codec] {
    def imap[A, B](fa: Codec[A])(f: A => B)(g: B => A): Codec[B] = fa.imap(f)(g)
  }
}
