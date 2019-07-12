package io.circe.generic.extras.decoding

import io.circe.{ Decoder, HCursor }
import shapeless.{ ::, Generic, HNil, Lazy }

abstract class UnwrappedDecoder[A] extends Decoder[A]

object UnwrappedDecoder {
  implicit def decodeUnwrapped[A, R](
    implicit
    gen: Lazy[Generic.Aux[A, R :: HNil]],
    decode: Decoder[R]
  ): UnwrappedDecoder[A] = new UnwrappedDecoder[A] {
    override def apply(c: HCursor): Decoder.Result[A] =
      decode(c) match {
        case Right(unwrapped) => Right(gen.value.from(unwrapped :: HNil))
        case l @ Left(_)      => l.asInstanceOf[Decoder.Result[A]]
      }
  }
}
