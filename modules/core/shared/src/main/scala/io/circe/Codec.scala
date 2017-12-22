package io.circe

/**
  *
  * Convenience typeclass to be used when you need both an [[io.circe.Encoder]] and
  * [[io.circe.Decoder]] for your type.
  *
  * @author Lorand Szakacs, lsz@lorandszakacs.com
  * @since 21 Dec 2017
  *
  */
trait Codec[A] extends Encoder[A] with Decoder[A]

object Codec {
  def apply[A](implicit instance: Codec[A]): Codec[A] = instance

  def instance[A](encode: Encoder[A], decode: Decoder[A]): Codec[A] =
    new Codec[A] {
      private val enc = encode
      private val dec = decode
      override def apply(a: A): Json = enc(a)
      override def apply(c: HCursor): Decoder.Result[A] = dec(c)
    }

  /**
    * Without this you cannot write functions that have [[io.circe.Codec]] typeclass
    * constraint where the codec can be derived from existing encoder + decoder pairs
    *
    * For instance:
    * {{{
    *   case class Box[A](a: A)
    *   implicit def codecBox[A: Codec]: Codec[Box[A]] = deriveCodec[Box[A]]
    * }}}
    *
    */
  implicit def summonCodecFromEncoderAndDecoder[A](implicit encode: Encoder[A], decode: Decoder[A]): Codec[A] =
    Codec.instance(encode, decode)
}
