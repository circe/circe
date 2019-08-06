package io.circe.shapes

import cats.data.Validated
import io.circe.{ Decoder, DecodingFailure, Encoder, HCursor }
import scala.collection.GenTraversable
import shapeless.{ AdditiveCollection, Nat, Sized }
import shapeless.ops.nat.ToInt

trait SizedInstances {
  implicit final def decodeSized[L <: Nat, C[X] <: GenTraversable[X], A](
    implicit
    decodeCA: Decoder[C[A]],
    ev: AdditiveCollection[C[A]],
    toInt: ToInt[L]
  ): Decoder[Sized[C[A], L]] = new Decoder[Sized[C[A], L]] {
    private[this] def checkSize(coll: C[A]): Option[Sized[C[A], L]] =
      if (coll.size == toInt()) Some(Sized.wrap[C[A], L](coll)) else None

    private[this] def failure(c: HCursor): DecodingFailure = DecodingFailure(s"Sized[C[A], _${toInt()}]", c.history)

    def apply(c: HCursor): Decoder.Result[Sized[C[A], L]] =
      decodeCA(c) match {
        case Right(as) =>
          checkSize(as) match {
            case Some(s) => Right(s)
            case None    => Left(failure(c))
          }
        case l @ Left(_) => l.asInstanceOf[Decoder.Result[Sized[C[A], L]]]
      }

    override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[Sized[C[A], L]] =
      decodeCA.decodeAccumulating(c) match {
        case Validated.Valid(as) =>
          checkSize(as) match {
            case Some(s) => Validated.valid(s)
            case None    => Validated.invalidNec(failure(c))
          }
        case l @ Validated.Invalid(_) => l.asInstanceOf[Decoder.AccumulatingResult[Sized[C[A], L]]]
      }
  }

  implicit def encodeSized[L <: Nat, C[_], A](implicit encodeCA: Encoder[C[A]]): Encoder[Sized[C[A], L]] =
    encodeCA.contramap(_.unsized)
}
