package io.circe.derivation

import scala.compiletime.{constValue, erasedValue, error, summonFrom, summonInline}
import scala.deriving.Mirror
import io.circe.{Decoder, Encoder, Codec}

inline final def summonLabels[T <: Tuple]: List[String] =
  inline erasedValue[T] match {
    case _: EmptyTuple => Nil
    case _: (t *: ts) => constValue[t].asInstanceOf[String] :: summonLabels[ts]
  }

inline final def summonEncoders[T <: Tuple]: List[Encoder[_]] =
  inline erasedValue[T] match {
    case _: EmptyTuple => Nil
    case _: (t *: ts) => summonEncoder[t] :: summonEncoders[ts]
  }

inline final def summonEncoder[A]: Encoder[A] =
  summonFrom {
    case encodeA: Encoder[A] => encodeA
    case _: Mirror.Of[A] => Encoder.AsObject.derived[A]
  }

inline final def summonDecoders[T <: Tuple]: List[Decoder[_]] =
  inline erasedValue[T] match {
    case _: EmptyTuple => Nil
    case _: (t *: ts) => summonDecoder[t] :: summonDecoders[ts]
  }

inline final def summonDecoder[A]: Decoder[A] =
  summonFrom {
    case decodeA: Decoder[A] => decodeA
    case _: Mirror.Of[A] => Decoder.derived[A]
  }

inline def summonSingletonCases[T <: Tuple, A](typeName: String): List[A] =
  inline erasedValue[T] match {
    case _: EmptyTuple => Nil
    case _: (h *: t) =>
      inline summonInline[Mirror.Of[h]] match {
        case m: Mirror.Singleton => m.fromProduct(EmptyTuple).asInstanceOf[A] :: summonSingletonCases[t, A](typeName)
        case m: Mirror => error("Enum " + typeName + " contains non singleton case " + constValue[m.MirroredLabel])
      }
  }