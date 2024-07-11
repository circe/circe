/*
 * Copyright 2024 circe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.circe.refined

import eu.timepit.refined.api.RefType
import eu.timepit.refined.api.Validate
import io.circe.Decoder
import io.circe.DecodingFailure
import io.circe.Encoder
import io.circe.KeyDecoder
import io.circe.KeyEncoder

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
            case r @ Right(_) => r.asInstanceOf[Decoder.Result[F[T, P]]]
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
