package io.circe

import cats.data.Xor
import eu.timepit.refined.api.{ RefType, Validate }

/**
 * Provides codecs for [[https://github.com/fthomas/refined refined]] types.
 *
 * A refined type `T Refined Predicate` is encoded as `T`. Decoding ensures that the
 * decoded value satisfies `Predicate`.
 *
 * E.g. with generic codecs
 * {{{
 *  case class Obj(
 *    i: Int Refined Positive
 *  )
 *
 *  Obj(refineMV(4)).asJson.nospaces == """{"i":4}"""
 * }}}
 *
 * @author Alexandre Archambault
 */
final object refined {
  implicit final def refinedDecoder[T, P, F[_, _]](implicit
    underlying: Decoder[T],
    validate: Validate[T, P],
    refType: RefType[F]
  ): Decoder[F[T, P]] =
    Decoder.instance { c =>
      underlying(c).flatMap { t0 =>
        refType.refine(t0) match {
          case Left(err) => Xor.Left(DecodingFailure(err, c.history))
          case Right(t)  => Xor.right(t)
        }
      }
    }

  implicit final def refinedEncoder[T, P, F[_, _]](implicit
    underlying: Encoder[T],
    refType: RefType[F]
  ): Encoder[F[T, P]] =
    underlying.contramap(refType.unwrap)
}
