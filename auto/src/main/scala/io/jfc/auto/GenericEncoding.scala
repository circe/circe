package io.jfc.auto

import io.jfc.{ Encoder, Json, JsonObject, ObjectEncoder }
import shapeless._, shapeless.labelled.FieldType

trait LowPriorityGenericEncoding {
  implicit def encodeHList[L <: HList](implicit
    productEncode: ProductEncoder[L]
  ): Encoder[L] = Encoder.instance(l => Json.fromValues(productEncode(l)))
}

trait GenericEncoding extends LowPriorityGenericEncoding with GenericDecoding {
  implicit val encodeHNil: ObjectEncoder[HNil] = ObjectEncoder.instance(_ => JsonObject.empty)

  implicit def encodeLabelledHList[K <: Symbol, H, T <: HList](implicit
    key: Witness.Aux[K],
    encodeHead: Lazy[Encoder[H]],
    encodeTail: Lazy[ObjectEncoder[T]]
  ): ObjectEncoder[FieldType[K, H] :: T] =
    ObjectEncoder.instance {
      case h :: t => (key.value.name -> encodeHead.value(h)) +: encodeTail.value.encodeObject(t)
    }

  implicit val encodeCNil: ObjectEncoder[CNil] = ObjectEncoder.instance(_ =>
    sys.error("JSON representation of CNil")
  )

  implicit def encodeCoproduct[K <: Symbol, H, T <: Coproduct](implicit
    key: Witness.Aux[K],
    encodeHead: Lazy[Encoder[H]],
    encodeTail: Lazy[ObjectEncoder[T]]
  ): ObjectEncoder[FieldType[K, H] :+: T] =
    ObjectEncoder.instance {
      case Inl(h) => JsonObject.singleton(key.value.name, encodeHead.value(h))
      case Inr(t) => encodeTail.value.encodeObject(t)
    }
}
