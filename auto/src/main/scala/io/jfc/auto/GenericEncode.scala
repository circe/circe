package io.jfc.auto

import io.jfc.{ Encode, Json }
import shapeless._, shapeless.labelled.FieldType

trait LowPriorityGenericEncode {
  implicit def encodeHList[L <: HList](implicit
    encodeArray: ArrayEncode[L]
  ): Encode[L] = Encode.instance(l => Json.fromValues(encodeArray(l)))
}

trait GenericEncode extends LowPriorityGenericEncode {
  implicit val encodeHNil: Encode[HNil] = Encode.instance(_ => Json.obj())

  implicit def encodeLabelledHList[K <: Symbol, H, T <: HList](implicit
    key: Witness.Aux[K],
    encodeHead: Lazy[Encode[H]],
    encodeTail: Lazy[Encode[T]]
  ): Encode[FieldType[K, H] :: T] =
    Encode.instance {
      case h :: t =>
        val tailJson = encodeTail.value(t)
        tailJson.asObject.fold(tailJson) { obj =>
          Json.fromJsonObject((key.value.name -> encodeHead.value(h)) +: obj)
        }
    }

  implicit val encodeCNil: Encode[CNil] = Encode.instance(_ =>
    sys.error("JSON representation of CNil")
  )

  implicit def encodeCoproduct[K <: Symbol, H, T <: Coproduct](implicit
    key: Witness.Aux[K],
    encodeHead: Lazy[Encode[H]],
    encodeTail: Lazy[Encode[T]]
  ): Encode[FieldType[K, H] :+: T] =
    Encode.instance {
      case Inl(h) => Json.obj(key.value.name -> encodeHead.value(h))
      case Inr(t) => encodeTail.value(t)
    }
}
