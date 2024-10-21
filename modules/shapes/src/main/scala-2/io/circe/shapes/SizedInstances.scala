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

package io.circe.shapes

import cats.data.Validated
import io.circe.Decoder
import io.circe.DecodingFailure
import io.circe.Encoder
import io.circe.HCursor
import shapeless.AdditiveCollection
import shapeless.Nat
import shapeless.Sized
import shapeless.ops.nat.ToInt

trait SizedInstances {
  implicit final def decodeSized[L <: Nat, C[X] <: Iterable[X], A](implicit
    decodeCA: Decoder[C[A]],
    ev: AdditiveCollection[C[A]],
    toInt: ToInt[L]
  ): Decoder[Sized[C[A], L]] = new Decoder[Sized[C[A], L]] {
    private[this] def checkSize(coll: C[A]): Option[Sized[C[A], L]] =
      if (coll.size == toInt()) Some(Sized.wrap[C[A], L](coll)) else None

    private[this] def failure(c: HCursor): DecodingFailure =
      DecodingFailure("Couldn't decode sized collection", c.history)

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
            case None    => Validated.invalidNel(failure(c))
          }
        case l @ Validated.Invalid(_) => l.asInstanceOf[Decoder.AccumulatingResult[Sized[C[A], L]]]
      }
  }

  implicit def encodeSized[L <: Nat, C[_], A](implicit encodeCA: Encoder[C[A]]): Encoder[Sized[C[A], L]] =
    encodeCA.contramap(_.unsized)
}
