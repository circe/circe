package io.circe.generic.simple.decoding

import io.circe.{ Decoder, HCursor }
import shapeless.LabelledGeneric

abstract class DerivedDecoder[A] extends Decoder[A]

object DerivedDecoder extends IncompleteDerivedDecoders {
  implicit def deriveDecoder[A, R](implicit
    gen: LabelledGeneric.Aux[A, R],
    decodeR: => ReprDecoder[R]
  ): DerivedDecoder[A] = new DerivedDecoder[A] {
    private[this] lazy val cachedDecodeR: Decoder[R] = decodeR

    final def apply(c: HCursor): Decoder.Result[A] = cachedDecodeR(c) match {
      case Right(r)    => Right(gen.from(r))
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[A]]
    }
    override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[A] =
      cachedDecodeR.decodeAccumulating(c).map(gen.from)
  }
}
