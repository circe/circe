package io.circe.generic

import io.circe.{ CursorOp, Decoder, DecodingFailure, HCursor }
import shapeless._

trait ProductDecoder[L <: HList] {
  def apply(history: List[CursorOp], js: List[HCursor]): Either[DecodingFailure, L]
}

object ProductDecoder {
  implicit val productDecodeHNil: ProductDecoder[HNil] = new ProductDecoder[HNil] {
    def apply(history: List[CursorOp], js: List[HCursor]): Either[DecodingFailure, HNil] =
      if (js.isEmpty) Right(HNil) else Left(
        DecodingFailure("Unexpected element in tuple", history)
      )
  }

  implicit def productDecodeHList[H, T <: HList](implicit
    decodeHead: Decoder[H],
    productDecodeTail: ProductDecoder[T]
  ): ProductDecoder[H :: T] = new ProductDecoder[H :: T] {
    def apply(history: List[CursorOp], js: List[HCursor]): Either[DecodingFailure, H :: T] =
      js match {
        case scala.collection.immutable.::(h, t) => for {
          head <- decodeHead(h).right
          tail <- productDecodeTail(history, t).right
        } yield head :: tail
        case Nil => Left(DecodingFailure("Not enough elements in tuple", history))
      }
  }
}
