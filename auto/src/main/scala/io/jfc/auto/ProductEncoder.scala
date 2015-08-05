package io.jfc.auto

import io.jfc.{ Encoder, Json }
import shapeless._

trait ProductEncoder[L <: HList] {
  def apply(l: L): List[Json]
}

object ProductEncoder {
  implicit val productEncodeHNil: ProductEncoder[HNil] = new ProductEncoder[HNil] {
    def apply(l: HNil): List[Json] = Nil
  }

  implicit def productEncodeHList[H, T <: HList](implicit
    encodeHead: Lazy[Encoder[H]],
    productEncodeTail: Lazy[ProductEncoder[T]]
  ): ProductEncoder[H :: T] = new ProductEncoder[H :: T] {
    def apply(l: H :: T): List[Json] = encodeHead.value(l.head) +: productEncodeTail.value(l.tail)
  }
}
