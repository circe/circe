package io.circe.generic.decoding

import io.circe.{ AccumulatingDecoder, Decoder, HCursor }
import shapeless.{ LabelledGeneric, Lazy }

abstract class DerivedDecoder[A] extends Decoder[A]

final object DerivedDecoder extends IncompleteDerivedDecoders {
  implicit def deriveDecoder[A, R](implicit
    gen: LabelledGeneric.Aux[A, R],
    decode: Lazy[ReprDecoder[R]]
  ): DerivedDecoder[A] = new DerivedDecoder[A] {
    final def apply(c: HCursor): Decoder.Result[A] = decode.value(c) match {
      case Right(r) => Right(gen.from(r))
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[A]]
    }
    override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] =
      decode.value.decodeAccumulating(c).map(gen.from)
  }
}
