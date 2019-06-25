package io.circe.generic.simple.codec

import io.circe.{ Codec, Decoder, HCursor, JsonObject }
import shapeless.LabelledGeneric

abstract class DerivedAsObjectCodec[A] extends Codec.AsObject[A]

final object DerivedAsObjectCodec {
  implicit def deriveCodec[A, R](
    implicit
    gen: LabelledGeneric.Aux[A, R],
    codecForR: => ReprAsObjectCodec[R]
  ): DerivedAsObjectCodec[A] = new DerivedAsObjectCodec[A] {
    private[this] lazy val cachedCodecForR: Codec.AsObject[R] = codecForR

    final def apply(c: HCursor): Decoder.Result[A] = cachedCodecForR.apply(c) match {
      case Right(r)    => Right(gen.from(r))
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[A]]
    }
    override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[A] =
      cachedCodecForR.decodeAccumulating(c).map(gen.from)

    final def encodeObject(a: A): JsonObject = cachedCodecForR.encodeObject(gen.to(a))
  }
}
