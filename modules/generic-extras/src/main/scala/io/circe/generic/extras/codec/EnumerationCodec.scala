package io.circe.generic.extras.codec

import io.circe.{ Codec, Decoder, DecodingFailure, HCursor, Json }
import io.circe.generic.extras.Configuration
import shapeless.{ :+:, CNil, Coproduct, HNil, Inl, Inr, LabelledGeneric, Witness }
import shapeless.labelled.{ FieldType, field }

abstract class EnumerationCodec[A] extends Codec[A]

object EnumerationCodec {
  implicit val codecForEnumerationCNil: EnumerationCodec[CNil] = new EnumerationCodec[CNil] {
    def apply(c: HCursor): Decoder.Result[CNil] = Left(DecodingFailure("Enumeration", c.history))
    def apply(a: CNil): Json = sys.error("Cannot encode CNil")
  }

  implicit def codecForEnumerationCCons[K <: Symbol, V, R <: Coproduct](
    implicit
    witK: Witness.Aux[K],
    gen: LabelledGeneric.Aux[V, HNil],
    codecForR: EnumerationCodec[R],
    config: Configuration = Configuration.default
  ): EnumerationCodec[FieldType[K, V] :+: R] = new EnumerationCodec[FieldType[K, V] :+: R] {
    def apply(c: HCursor): Decoder.Result[FieldType[K, V] :+: R] =
      c.as[String] match {
        case Right(s) if s == config.transformConstructorNames(witK.value.name) =>
          Right(Inl(field[K](gen.from(HNil))))
        case Right(_) =>
          codecForR.apply(c) match {
            case Right(v)  => Right(Inr(v))
            case Left(err) => Left(err)
          }
        case Left(err) => Left(DecodingFailure("Enumeration", c.history))
      }
    def apply(a: FieldType[K, V] :+: R): Json = a match {
      case Inl(l) => Json.fromString(config.transformConstructorNames(witK.value.name))
      case Inr(r) => codecForR(r)
    }
  }

  implicit def codecForEnumeration[A, R <: Coproduct](
    implicit
    gen: LabelledGeneric.Aux[A, R],
    codecForR: EnumerationCodec[R]
  ): EnumerationCodec[A] =
    new EnumerationCodec[A] {
      def apply(c: HCursor): Decoder.Result[A] = codecForR(c) match {
        case Right(v)  => Right(gen.from(v))
        case Left(err) => Left(err)
      }
      def apply(a: A): Json = codecForR(gen.to(a))
    }
}
