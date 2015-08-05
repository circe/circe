package io.jfc.auto

import cats.data.Xor
import io.jfc.{ CursorOp, Decoder, DecodingFailure, HCursor, Json }
import shapeless._

trait ProductDecoder[L <: HList] {
  def apply(history: List[CursorOp], js: List[HCursor]): Xor[DecodingFailure, L]
}

object ProductDecoder {
  implicit val productDecodeHNil: ProductDecoder[HNil] = new ProductDecoder[HNil] {
    def apply(history: List[CursorOp], js: List[HCursor]): Xor[DecodingFailure, HNil] =
      if (js.isEmpty) Xor.right(HNil) else Xor.left(
        DecodingFailure("Unexpected element in tuple", history)
      )
  }

  implicit def productDecodeHList[H, T <: HList](implicit
    decodeHead: Lazy[Decoder[H]],
    productDecodeTail: Lazy[ProductDecoder[T]]
  ): ProductDecoder[H :: T] = new ProductDecoder[H :: T] {
    def apply(history: List[CursorOp], js: List[HCursor]): Xor[DecodingFailure, H :: T] = js match {
      case scala.collection.immutable.::(h, t) => for {
        head <- decodeHead.value(h)
        tail <- productDecodeTail.value(history, t)
      } yield head :: tail
      case Nil => Xor.left(DecodingFailure("Not enough elements in tuple", history))
    }
  }
}
