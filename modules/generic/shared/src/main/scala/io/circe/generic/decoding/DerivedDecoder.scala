package io.circe.generic.decoding

import io.circe.{ AccumulatingDecoder, Decoder, HCursor }
import io.circe.generic.DerivationMacros
import scala.language.experimental.macros
import shapeless.{ Coproduct, HList, LabelledGeneric, Lazy }

abstract class DerivedDecoder[A] extends Decoder[A]

@export.exports
final object DerivedDecoder extends IncompleteDerivedDecoders {
  implicit def decodeHList[R <: HList]: DerivedDecoder[R] = macro DerivationMacros.decodeHList[R]
  implicit def decodeCoproduct[R <: Coproduct]: DerivedDecoder[R] = macro DerivationMacros.decodeCoproduct[R]

  implicit def decodeCaseClass[A, R <: HList](implicit
    gen: LabelledGeneric.Aux[A, R],
    decode: Lazy[DerivedDecoder[R]]
  ): DerivedDecoder[A] = new DerivedDecoder[A] {
    final def apply(c: HCursor): Decoder.Result[A] = decode.value(c) match {
      case Right(r) => Right(gen.from(r))
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[A]]
    }
    override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] =
      decode.value.decodeAccumulating(c).map(gen.from)
  }

  implicit def decodeAdt[A, R <: Coproduct](implicit
    gen: LabelledGeneric.Aux[A, R],
    decode: Lazy[DerivedDecoder[R]]
  ): DerivedDecoder[A] = new DerivedDecoder[A] {
    final def apply(c: HCursor): Decoder.Result[A] = decode.value(c) match {
      case Right(r) => Right(gen.from(r))
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[A]]
    }
    override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] =
      decode.value.decodeAccumulating(c).map(gen.from)
  }
}
