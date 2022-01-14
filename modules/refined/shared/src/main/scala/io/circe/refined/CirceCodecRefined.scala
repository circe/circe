package io.circe.refined

import eu.timepit.refined.api.{ RefType, Validate }
import io.circe.{ Decoder, DecodingFailure, Encoder, KeyDecoder, KeyEncoder }

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
 *  Obj(refineMV(4)).asJson.noSpaces == """{"i":4}"""
 * }}}
 *
 * @author Alexandre Archambault
 */
trait CirceCodecRefined {
  implicit final def refinedDecoder[T, P, F[_, _]](implicit
    underlying: Decoder[T],
    validate: Validate[T, P],
    refType: RefType[F]
  ): Decoder[F[T, P]] =
    Decoder.instance { c =>
      underlying(c) match {
        case Right(t0) =>
          refType.refine(t0) match {
            case Left(err)    => Left(DecodingFailure(err, c.history))
            case r @ Right(t) => r.asInstanceOf[Decoder.Result[F[T, P]]]
          }
        case l @ Left(_) => l.asInstanceOf[Decoder.Result[F[T, P]]]
      }
    }

  implicit final def refinedEncoder[T, P, F[_, _]](implicit
    underlying: Encoder[T],
    refType: RefType[F]
  ): Encoder[F[T, P]] =
    underlying.contramap(refType.unwrap)

  implicit final def refinedKeyDecoder[T, P, F[_, _]](implicit
    underlying: KeyDecoder[T],
    validate: Validate[T, P],
    refType: RefType[F]
  ): KeyDecoder[F[T, P]] =
    KeyDecoder.instance { str =>
      underlying(str).flatMap { t0 =>
        refType.refine(t0) match {
          case Left(_)  => None
          case Right(t) => Some(t)
        }
      }
    }

  implicit final def refinedKeyEncoder[T, P, F[_, _]](implicit
    underlying: KeyEncoder[T],
    refType: RefType[F]
  ): KeyEncoder[F[T, P]] =
    underlying.contramap(refType.unwrap)
}
