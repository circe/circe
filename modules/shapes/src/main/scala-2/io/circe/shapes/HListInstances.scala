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

import io.circe.Decoder
import io.circe.DecodingFailure
import io.circe.DecodingFailure.Reason.WrongTypeExpectation
import io.circe.Encoder
import io.circe.HCursor
import io.circe.Json
import io.circe.JsonObject
import shapeless.::
import shapeless.HList
import shapeless.HNil

trait HListInstances extends LowPriorityHListInstances {
  implicit final def decodeSingletonHList[H](implicit decodeH: Decoder[H]): Decoder[H :: HNil] =
    Decoder[Tuple1[H]].map(t => t._1 :: HNil).withErrorMessage("HList")

  implicit final def encodeSingletonHList[H](implicit encodeH: Encoder[H]): Encoder.AsArray[H :: HNil] =
    new Encoder.AsArray[H :: HNil] {
      def encodeArray(a: H :: HNil): Vector[Json] = Vector(encodeH(a.head))
    }
}

private[shapes] trait LowPriorityHListInstances {
  implicit final val decodeHNil: Decoder[HNil] = new Decoder[HNil] {
    def apply(c: HCursor): Decoder.Result[HNil] =
      if (c.value.isObject) Right(HNil)
      else Left(DecodingFailure(WrongTypeExpectation("object", c.value), c.history))
  }

  implicit final val encodeHNil: Encoder.AsObject[HNil] = new Encoder.AsObject[HNil] {
    def encodeObject(a: HNil): JsonObject = JsonObject.empty
  }

  implicit final def decodeHCons[H, T <: HList](implicit
    decodeH: Decoder[H],
    decodeT: Decoder[T]
  ): Decoder[H :: T] = new Decoder[H :: T] {
    def apply(c: HCursor): Decoder.Result[H :: T] = {
      val first = c.downArray

      Decoder.resultInstance.map2(first.as(decodeH), decodeT.tryDecode(first.delete))(_ :: _)
    }

    override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[H :: T] = {
      val first = c.downArray

      Decoder.accumulatingResultInstance.map2(
        decodeH.tryDecodeAccumulating(first),
        decodeT.tryDecodeAccumulating(first.delete)
      )(_ :: _)
    }
  }

  implicit final def encodeHCons[H, T <: HList](implicit
    encodeH: Encoder[H],
    encodeT: Encoder.AsArray[T]
  ): Encoder.AsArray[H :: T] = new Encoder.AsArray[H :: T] {
    def encodeArray(a: H :: T): Vector[Json] = encodeH(a.head) +: encodeT.encodeArray(a.tail)
  }
}
