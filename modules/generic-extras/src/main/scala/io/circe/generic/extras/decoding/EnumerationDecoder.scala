package io.circe.generic.extras.decoding

import io.circe.{ Decoder, DecodingFailure, HCursor }
import shapeless.{ :+:, CNil, Coproduct, HNil, Inl, Inr, LabelledGeneric, Witness }
import shapeless.labelled.{ field, FieldType }

abstract class EnumerationDecoder[A] extends Decoder[A]

final object EnumerationDecoder {
  implicit val decodeEnumerationCNil: EnumerationDecoder[CNil] = new EnumerationDecoder[CNil] {
    def apply(c: HCursor): Decoder.Result[CNil] = Left(DecodingFailure("Enumeration", c.history))
  }

  implicit def decodeEnumerationCCons[K <: Symbol, V, R <: Coproduct](
    implicit
    wit: Witness.Aux[K],
    gv: LabelledGeneric.Aux[V, HNil],
    dr: EnumerationDecoder[R]
  ): EnumerationDecoder[FieldType[K, V] :+: R] = new EnumerationDecoder[FieldType[K, V] :+: R] {
    def apply(c: HCursor): Decoder.Result[FieldType[K, V] :+: R] =
      c.as[String] match {
        case Right(s) if s == wit.value.name => Right(Inl(field[K](gv.from(HNil))))
        case Right(_) =>
          dr.apply(c) match {
            case Right(v)  => Right(Inr(v))
            case Left(err) => Left(err)
          }
        case Left(err) => Left(DecodingFailure("Enumeration", c.history))
      }
  }

  implicit def decodeEnumeration[A, Repr <: Coproduct](
    implicit
    gen: LabelledGeneric.Aux[A, Repr],
    rr: EnumerationDecoder[Repr]
  ): EnumerationDecoder[A] =
    new EnumerationDecoder[A] {
      def apply(c: HCursor): Decoder.Result[A] = rr(c) match {
        case Right(v)  => Right(gen.from(v))
        case Left(err) => Left(err)
      }
    }
}
