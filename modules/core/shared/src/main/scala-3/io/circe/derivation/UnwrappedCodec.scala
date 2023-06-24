package io.circe.derivation

import io.circe._
import scala.deriving.Mirror

trait UnwrappedCodec[A] extends Codec[A] with UnwrappedEncoder[A] with UnwrappedDecoder[A]

object UnwrappedCodec {
  inline final def derived[A](using inline mirror: Mirror.Of[A]): UnwrappedCodec[A] = {
    val encodeA = UnwrappedEncoder.derived[A]
    val decodeA = UnwrappedDecoder.derived[A]
    new UnwrappedCodec[A] {
      override def apply(a: A): Json = encodeA(a)
      override def apply(c: HCursor): Decoder.Result[A] = decodeA(c)
    }
  }
}
