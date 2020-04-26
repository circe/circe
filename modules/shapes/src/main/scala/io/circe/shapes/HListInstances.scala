package io.circe.shapes

import io.circe.{ Decoder, DecodingFailure, Encoder, HCursor, Json, JsonObject }
import shapeless.{ ::, HList, HNil }

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
      if (c.value.isObject) Right(HNil) else Left(DecodingFailure("HNil", c.history))
  }

  implicit final val encodeHNil: Encoder.AsObject[HNil] = new Encoder.AsObject[HNil] {
    def encodeObject(a: HNil): JsonObject[Json] = JsonObject.empty
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
