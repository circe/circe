package io.circe.shapes

import io.circe.{ AccumulatingDecoder, ArrayEncoder, Decoder, Encoder, HCursor, Json, JsonObject, ObjectEncoder }
import shapeless.{ ::, HList, HNil }

trait HListInstances extends LowPriorityHListInstances {
  implicit final def decodeSingletonHList[H](implicit decodeH: Decoder[H]): Decoder[H :: HNil] =
    Decoder[Tuple1[H]].map(t => t._1 :: HNil).withErrorMessage("HList")

  implicit final def encodeSingletonHList[H](implicit encodeH: Encoder[H]): ArrayEncoder[H :: HNil] =
    new ArrayEncoder[H :: HNil] {
      def encodeArray(a: H :: HNil): List[Json] = List(encodeH(a.head))
    }
}

private[shapes] trait LowPriorityHListInstances {
  implicit final val decodeHNil: Decoder[HNil] = Decoder.const(HNil)
  implicit final val encodeHNil: ObjectEncoder[HNil] = new ObjectEncoder[HNil] {
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

    override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[H :: T] = {
      val first = c.downArray

      AccumulatingDecoder.resultInstance.map2(
        decodeH.tryDecodeAccumulating(first),
        decodeT.tryDecodeAccumulating(first.delete)
      )(_ :: _)
    }
  }

  implicit final def encodeHCons[H, T <: HList](implicit
    encodeH: Encoder[H],
    encodeT: ArrayEncoder[T]
  ): ArrayEncoder[H :: T] = new ArrayEncoder[H :: T] {
      def encodeArray(a: H :: T): List[Json] = encodeH(a.head) +: encodeT.encodeArray(a.tail)
  }
}
