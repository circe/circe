package io.circe.generic.extras.encoding

import io.circe.{ Encoder, Json }
import io.circe.generic.extras.Configuration
import shapeless.{ :+:, CNil, Coproduct, HNil, Inl, Inr, LabelledGeneric, Witness }
import shapeless.labelled.FieldType

abstract class EnumerationEncoder[A] extends Encoder[A]

object EnumerationEncoder {
  implicit val encodeEnumerationCNil: EnumerationEncoder[CNil] = new EnumerationEncoder[CNil] {
    def apply(a: CNil): Json = sys.error("Cannot encode CNil")
  }

  implicit def encodeEnumerationCCons[K <: Symbol, V, R <: Coproduct](
    implicit
    wit: Witness.Aux[K],
    gv: LabelledGeneric.Aux[V, HNil],
    dr: EnumerationEncoder[R],
    config: Configuration = Configuration.default
  ): EnumerationEncoder[FieldType[K, V] :+: R] = new EnumerationEncoder[FieldType[K, V] :+: R] {
    def apply(a: FieldType[K, V] :+: R): Json = a match {
      case Inl(l) => Json.fromString(config.transformConstructorNames(wit.value.name))
      case Inr(r) => dr(r)
    }
  }

  implicit def encodeEnumeration[A, Repr <: Coproduct](
    implicit
    gen: LabelledGeneric.Aux[A, Repr],
    rr: EnumerationEncoder[Repr]
  ): EnumerationEncoder[A] =
    new EnumerationEncoder[A] {
      def apply(a: A): Json = rr(gen.to(a))
    }
}
