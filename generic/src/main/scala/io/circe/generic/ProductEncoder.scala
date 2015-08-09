package io.circe.generic

import io.circe.{ Encoder, Json }
import shapeless._

trait ProductEncoder[L <: HList] {
  def apply(l: L): List[Json]
}

object ProductEncoder {
  implicit val productEncodeHNil: ProductEncoder[HNil] = new ProductEncoder[HNil] {
    def apply(l: HNil): List[Json] = Nil
  }

  implicit def productEncodeHList[H, T <: HList](implicit
    encodeHead: Encoder[H],
    productEncodeTail: ProductEncoder[T]
  ): ProductEncoder[H :: T] = new ProductEncoder[H :: T] {
    def apply(l: H :: T): List[Json] = encodeHead(l.head) +: productEncodeTail(l.tail)
  }
}
