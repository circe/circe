package io.circe.generic

import io.circe.{ Decoder, Encoder, HCursor, Json }
import shapeless.HList

trait HListInstances {
  implicit def decodeHList[L <: HList](implicit
    productDecode: ProductDecoder[L]
  ): Decoder[L] = Decoder.instance { c =>
    c.as[List[HCursor]].right.flatMap(js => productDecode(c.history, js))
  }

  implicit def encodeHList[L <: HList](implicit
    e: ProductEncoder[L]
  ): Encoder[L] = Encoder.instance(l => Json.fromValues(e(l)))
}
