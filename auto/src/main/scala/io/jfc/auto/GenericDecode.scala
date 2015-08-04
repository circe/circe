package io.jfc.auto

import cats.data.Xor
import io.jfc.{ Decode, DecodeFailure, HCursor, Json }
import shapeless._, shapeless.labelled.{ FieldType, field }

trait LowPriorityGenericDecode {
  implicit def decodeHList[L <: HList](implicit
    decodeArray: ArrayDecode[L]
  ): Decode[L] = Decode.instance { c =>
    c.as[List[HCursor]].flatMap(js => decodeArray(c.history, js))
  }

  implicit val decodeCNil: Decode[CNil] =
    Decode.instance(c => Xor.left(DecodeFailure("CNil", c.history)))

}

trait GenericDecode extends LowPriorityGenericDecode {
  implicit val decodeHNil: Decode[HNil] = Decode.instance(_ => Xor.right(HNil))

  implicit def decodeLabelledHList[K <: Symbol, H, T <: HList](implicit
    key: Witness.Aux[K],
    decodeHead: Lazy[Decode[H]],
    decodeTail: Lazy[Decode[T]]
  ): Decode[FieldType[K, H] :: T] =
    Decode.instance { c =>
      for {
        head <- c.get(key.value.name)(decodeHead.value)
        tail <- c.as(decodeTail.value)
      } yield field[K](head) :: tail
    }

  implicit def decodeCoproduct[K <: Symbol, H, T <: Coproduct](implicit
    key: Witness.Aux[K],
    decodeHead: Lazy[Decode[H]],
    decodeTail: Lazy[Decode[T]]
  ): Decode[FieldType[K, H] :+: T] =
    Decode.instance { c =>
      c.downField(key.value.name).focus.fold[Xor[DecodeFailure, FieldType[K, H] :+: T]](
        decodeTail.value(c).map(Inr(_))
      ) { headJson =>
        headJson.as(decodeHead.value).map(h => Inl(field(h)))
      }
    }
}
