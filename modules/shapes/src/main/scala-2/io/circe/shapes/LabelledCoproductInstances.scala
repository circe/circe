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

import cats.kernel.Eq
import io.circe.Decoder
import io.circe.DecodingFailure
import io.circe.Encoder
import io.circe.HCursor
import io.circe.Json
import io.circe.KeyDecoder
import io.circe.KeyEncoder
import shapeless.:+:
import shapeless.Coproduct
import shapeless.Inl
import shapeless.Inr
import shapeless.Widen
import shapeless.Witness
import shapeless.labelled.FieldType
import shapeless.labelled.field

trait LabelledCoproductInstances extends LowPriorityLabelledCoproductInstances {
  implicit final def decodeSymbolLabelledCCons[K <: Symbol, V, R <: Coproduct](implicit
    witK: Witness.Aux[K],
    decodeV: Decoder[V],
    decodeR: Decoder[R]
  ): Decoder[FieldType[K, V] :+: R] = new Decoder[FieldType[K, V] :+: R] {
    def apply(c: HCursor): Decoder.Result[FieldType[K, V] :+: R] =
      Decoder.resultSemigroupK.combineK(
        c.get[V](witK.value.name).map(v => Inl(field[K](v))),
        decodeR(c).map(Inr(_))
      )
  }

  implicit final def encodeSymbolLabelledCCons[K <: Symbol, V, R <: Coproduct](implicit
    witK: Witness.Aux[K],
    encodeV: Encoder[V],
    encodeR: Encoder[R]
  ): Encoder[FieldType[K, V] :+: R] = new Encoder[FieldType[K, V] :+: R] {
    def apply(a: FieldType[K, V] :+: R): Json = a match {
      case Inl(l) => Json.obj((witK.value.name, encodeV(l)))
      case Inr(r) => encodeR(r)
    }
  }
}

private[shapes] trait LowPriorityLabelledCoproductInstances extends CoproductInstances {
  implicit final def decodeLabelledCCons[K, W >: K, V, R <: Coproduct](implicit
    witK: Witness.Aux[K],
    widenK: Widen.Aux[K, W],
    eqW: Eq[W],
    decodeW: KeyDecoder[W],
    decodeV: Decoder[V],
    decodeR: Decoder[R]
  ): Decoder[FieldType[K, V] :+: R] = new Decoder[FieldType[K, V] :+: R] {
    private[this] val widened = widenK(witK.value)
    private[this] val isK: String => Boolean = decodeW(_).exists(eqW.eqv(widened, _))

    def apply(c: HCursor): Decoder.Result[FieldType[K, V] :+: R] =
      Decoder.resultSemigroupK.combineK(
        c.keys
          .flatMap(_.find(isK))
          .fold[Decoder.Result[String]](
            Left(DecodingFailure("Record", c.history))
          )(Right(_))
          .flatMap(c.get[V](_).map(v => Inl(field[K](v)))),
        decodeR(c).map(Inr(_))
      )
  }

  implicit final def encodeLabelledCCons[K, W >: K, V, R <: Coproduct](implicit
    witK: Witness.Aux[K],
    eqW: Eq[W],
    encodeW: KeyEncoder[W],
    encodeV: Encoder[V],
    encodeR: Encoder[R]
  ): Encoder[FieldType[K, V] :+: R] = new Encoder[FieldType[K, V] :+: R] {
    def apply(a: FieldType[K, V] :+: R): Json = a match {
      case Inl(l) => Json.obj((encodeW(witK.value), encodeV(l)))
      case Inr(r) => encodeR(r)
    }
  }
}
