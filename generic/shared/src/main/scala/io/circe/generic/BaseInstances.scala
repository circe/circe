package io.circe.generic

import cats.data.Xor
import io.circe.{ Decoder, DecodingFailure, JsonObject, ObjectEncoder }
import shapeless.{ CNil, HNil }

trait BaseInstances {
  implicit val decodeHNil: Decoder[HNil] = Decoder.instance(_ => Xor.right(HNil))
  implicit val decodeCNil: Decoder[CNil] =
    Decoder.instance(c => Xor.left(DecodingFailure("CNil", c.history)))

  implicit val encodeHNil: ObjectEncoder[HNil] = ObjectEncoder.instance(_ => JsonObject.empty)
  implicit val encodeCNil: ObjectEncoder[CNil] = ObjectEncoder.instance(_ =>
    sys.error("No JSON representation of CNil (this shouldn't happen)")
  )
}
