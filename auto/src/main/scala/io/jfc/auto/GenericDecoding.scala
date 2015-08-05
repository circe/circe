package io.jfc.auto

import cats.data.Xor
import io.jfc.{ Decoder, DecodingFailure, HCursor, Json }
import shapeless._, shapeless.labelled.{ FieldType, field }

trait LowPriorityGenericDecoding {
  implicit def decodeHList[L <: HList](implicit
    productDecode: ProductDecoder[L]
  ): Decoder[L] = Decoder.instance { c =>
    c.as[List[HCursor]].flatMap(js => productDecode(c.history, js))
  }

  implicit val decodeCNil: Decoder[CNil] =
    Decoder.instance(c => Xor.left(DecodingFailure("CNil", c.history)))

}

trait GenericDecoding extends LowPriorityGenericDecoding {
  implicit val decodeHNil: Decoder[HNil] = Decoder.instance(_ => Xor.right(HNil))

  implicit def decodeLabelledHList[K <: Symbol, H, T <: HList](implicit
    key: Witness.Aux[K],
    decodeHead: Lazy[Decoder[H]],
    decodeTail: Lazy[Decoder[T]]
  ): Decoder[FieldType[K, H] :: T] =
    Decoder.instance { c =>
      for {
        head <- c.get(key.value.name)(decodeHead.value)
        tail <- c.as(decodeTail.value)
      } yield field[K](head) :: tail
    }

  implicit def decodeCoproduct[K <: Symbol, H, T <: Coproduct](implicit
    key: Witness.Aux[K],
    decodeHead: Lazy[Decoder[H]],
    decodeTail: Lazy[Decoder[T]]
  ): Decoder[FieldType[K, H] :+: T] =
    Decoder.instance { c =>
      c.downField(key.value.name).focus.fold[Xor[DecodingFailure, FieldType[K, H] :+: T]](
        decodeTail.value(c).map(Inr(_))
      ) { headJson =>
        headJson.as(decodeHead.value).map(h => Inl(field(h)))
      }
    }
}
