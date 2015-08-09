package io.jfc.generic

import cats.data.Xor
import io.jfc.{ Decoder, DecodingFailure, Encoder, JsonObject, ObjectEncoder }
import shapeless._, shapeless.labelled.{ FieldType, field }

trait LabelledInstances {
  implicit def decodeCoproduct[K <: Symbol, H, HR, T <: Coproduct](implicit
    key: Witness.Aux[K],
    gen: LabelledGeneric.Aux[H, HR],
    decodeHead: Lazy[Decoder[HR]],
    decodeTail: Decoder[T]
  ): Decoder[FieldType[K, H] :+: T] =
    Decoder.instance { c =>
      c.downField(key.value.name).focus.fold[Xor[DecodingFailure, FieldType[K, H] :+: T]](
        decodeTail(c).map(Inr(_))
      ) { headJson =>
        headJson.as(decodeHead.value).map(h => Inl(field(gen.from(h))))
      }
    }

  implicit def decodeLabelledHList[K <: Symbol, H, T <: HList](implicit
    key: Witness.Aux[K],
    decodeHead: Decoder[H],
    decodeTail: Decoder[T]
  ): Decoder[FieldType[K, H] :: T] =
    Decoder.instance { c =>
      for {
        head <- c.get(key.value.name)(decodeHead)
        tail <- c.as(decodeTail)
      } yield field[K](head) :: tail
    }

  implicit def encodeCoproduct[K <: Symbol, H, HR, T <: Coproduct](implicit
    key: Witness.Aux[K],
    gen: LabelledGeneric.Aux[H, HR],
    encodeHead: Lazy[Encoder[HR]],
    encodeTail: ObjectEncoder[T]
  ): ObjectEncoder[FieldType[K, H] :+: T] =
    ObjectEncoder.instance {
      case Inl(h) => JsonObject.singleton(key.value.name, encodeHead.value(gen.to(h)))
      case Inr(t) => encodeTail.encodeObject(t)
    }

  implicit def encodeLabelledHList[K <: Symbol, H, T <: HList](implicit
    key: Witness.Aux[K],
    encodeHead: Encoder[H],
    encodeTail: ObjectEncoder[T]
  ): ObjectEncoder[FieldType[K, H] :: T] =
    ObjectEncoder.instance {
      case h :: t =>
      (key.value.name -> encodeHead(h)) +: encodeTail.encodeObject(t)
    }
}
