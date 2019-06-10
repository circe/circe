package io.circe.generic.codec

import io.circe.{ Codec, Decoder, HCursor, JsonObject }
import shapeless.{ LabelledGeneric, Lazy }

abstract class DerivedAsObjectCodec[A] extends Codec.AsObject[A]

final object DerivedAsObjectCodec {
  implicit def deriveCodec[A, R](
    implicit
    gen: LabelledGeneric.Aux[A, R],
    codec: Lazy[ReprAsObjectCodec[R]]
  ): DerivedAsObjectCodec[A] = new DerivedAsObjectCodec[A] {
    private[this] val reprCodec: ReprAsObjectCodec[R] = codec.value

    final def apply(c: HCursor): Decoder.Result[A] = reprCodec.apply(c) match {
      case Right(r)    => Right(gen.from(r))
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[A]]
    }
    override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[A] =
      reprCodec.decodeAccumulating(c).map(gen.from)

    final def encodeObject(a: A): JsonObject = reprCodec.encodeObject(gen.to(a))
  }
}
