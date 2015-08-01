package io.jfc.auto

import cats.data.Xor
import io.jfc.{ CursorHistory, Decode, DecodeFailure, HCursor, Json }
import shapeless._

trait ArrayDecode[L <: HList] {
  def apply(history: CursorHistory, js: List[HCursor]): Xor[DecodeFailure, L]
}

object ArrayDecode {
  implicit val arrayDecodeHNil: ArrayDecode[HNil] = new ArrayDecode[HNil] {
    def apply(history: CursorHistory, js: List[HCursor]): Xor[DecodeFailure, HNil] =
      if (js.isEmpty) Xor.right(HNil) else Xor.left(
        DecodeFailure("Unexpected element in tuple", history)
      )
  }

  implicit def arrayDecodeHList[H, T <: HList](implicit
    decodeHead: Lazy[Decode[H]],
    arrayDecodeTail: Lazy[ArrayDecode[T]]
  ): ArrayDecode[H :: T] = new ArrayDecode[H :: T] {
    def apply(history: CursorHistory, js: List[HCursor]): Xor[DecodeFailure, H :: T] = js match {
      case scala.collection.immutable.::(h, t) => for {
        head <- decodeHead.value(h)
        tail <- arrayDecodeTail.value(history, t)
      } yield head :: tail
      case Nil => Xor.left(DecodeFailure("Not enough elements in tuple", history))
    }
  }
}
