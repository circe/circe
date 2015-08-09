package io.jfc.generic

import io.jfc.{ Decoder, Encoder, ObjectEncoder }
import shapeless.{ Coproduct, HList, LabelledGeneric }

trait GenericInstances {
  implicit def decodeAdt[A, R <: Coproduct](implicit
    gen: LabelledGeneric.Aux[A, R],
    d: Decoder[R]
  ): Decoder.Secondary[A] = new Decoder.Secondary(d.map(gen.from))

  implicit def decodeCaseClass[A, R <: HList](implicit
    gen: LabelledGeneric.Aux[A, R],
    d: Decoder[R]
  ): Decoder.Secondary[A] = new Decoder.Secondary(d.map(gen.from))

  implicit def encodeAdt[A, R <: Coproduct](implicit
    gen: LabelledGeneric.Aux[A, R],
    e: ObjectEncoder[R]
  ): Encoder.Secondary[A] = new Encoder.Secondary(e.contramap(gen.to))

  implicit def encodeCaseClass[A, R <: HList](implicit
    gen: LabelledGeneric.Aux[A, R],
    e: Encoder[R]
  ): Encoder.Secondary[A] = new Encoder.Secondary(e.contramap(gen.to))
}
