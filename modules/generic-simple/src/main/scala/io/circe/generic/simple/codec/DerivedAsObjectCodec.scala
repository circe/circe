package io.circe.generic.simple.codec

import io.circe.{ Codec, Decoder, HCursor, JsonObject }
import shapeless.LabelledGeneric

abstract class DerivedAsObjectCodec[A] extends Codec.AsObject[A]

final object DerivedAsObjectCodec {
  implicit def deriveCodec[A, R](
    implicit
    gen: LabelledGeneric.Aux[A, R],
    codec: => ReprAsObjectCodec[R]
  ): DerivedAsObjectCodec[A] = new DerivedAsObjectCodec[A] {
    final def apply(c: HCursor): Decoder.Result[A] = codec.apply(c) match {
      case Right(r)    => Right(gen.from(r))
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[A]]
    }
    override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[A] =
      codec.decodeAccumulating(c).map(gen.from)

    final def encodeObject(a: A): JsonObject = codec.encodeObject(gen.to(a))
  }
}
