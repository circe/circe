package io.circe.generic.extras.codec

import io.circe.{ Codec, Decoder, Encoder, HCursor, Json }
import shapeless.{ ::, Generic, HNil, Lazy }

abstract class UnwrappedCodec[A] extends Codec[A]

object UnwrappedCodec {
  implicit def codecForUnwrapped[A, R](
    implicit
    gen: Lazy[Generic.Aux[A, R :: HNil]],
    decodeR: Decoder[R],
    encodeR: Encoder[R]
  ): UnwrappedCodec[A] = new UnwrappedCodec[A] {

    override def apply(c: HCursor): Decoder.Result[A] =
      decodeR(c) match {
        case Right(unwrapped) => Right(gen.value.from(unwrapped :: HNil))
        case l @ Left(_)      => l.asInstanceOf[Decoder.Result[A]]
      }
    override def apply(a: A): Json =
      encodeR(gen.value.to(a).head)
  }
}
