package io.circe.generic.extras.util

import scala.collection.immutable.Map
import shapeless.{ ::, HList, HNil, Witness }
import shapeless.labelled.FieldType

abstract class RecordToMap[R <: HList] {
  def apply(r: R): Map[String, Any]
}

final object RecordToMap {
  implicit val hnilRecordToMap: RecordToMap[HNil] = new RecordToMap[HNil] {
    def apply(r: HNil): Map[String, Any] = Map.empty
  }

  implicit def hconsRecordToMap[K <: Symbol, V, T <: HList](implicit
    wit: Witness.Aux[K],
    rtmT: RecordToMap[T]
  ): RecordToMap[FieldType[K, V] :: T] = new RecordToMap[FieldType[K, V] :: T] {
    def apply(r: FieldType[K, V] :: T): Map[String, Any] = rtmT(r.tail) + ((wit.value.name, r.head))
  }
}
