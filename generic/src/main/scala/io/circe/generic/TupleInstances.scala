package io.circe.generic

import io.circe.{ Decoder, Encoder }
import shapeless.{ HList, IsTuple, Generic }

trait TupleInstances {
  implicit def decodeTuple[A, R <: HList](implicit
    isTuple: IsTuple[A],
    gen: Generic.Aux[A, R],
    d: Decoder[R]
  ): Decoder[A] = d.map(gen.from)

  implicit def encodeTuple[A, R <: HList](implicit
    isTuple: IsTuple[A],
    gen: Generic.Aux[A, R],
    e: Encoder[R]
  ): Encoder[A] = e.contramap(gen.to)
}
