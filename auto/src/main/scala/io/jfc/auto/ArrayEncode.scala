package io.jfc.auto

import io.jfc.{ Encode, Json }
import shapeless._

trait ArrayEncode[L <: HList] {
  def apply(l: L): List[Json]
}

object ArrayEncode {
  implicit val arrayEncodeHNil: ArrayEncode[HNil] = new ArrayEncode[HNil] {
    def apply(l: HNil): List[Json] = Nil
  }

  implicit def arrayEncodeHList[H, T <: HList](implicit
    encodeHead: Lazy[Encode[H]],
    arrayEncodeTail: Lazy[ArrayEncode[T]]
  ): ArrayEncode[H :: T] = new ArrayEncode[H :: T] {
    def apply(l: H :: T): List[Json] = encodeHead.value(l.head) +: arrayEncodeTail.value(l.tail)
  }
}
