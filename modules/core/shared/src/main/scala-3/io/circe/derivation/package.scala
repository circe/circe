package io.circe.derivation

import scala.compiletime.{constValue, erasedValue, summonFrom}
import scala.deriving.Mirror
import io.circe.{Encoder, Decoder}

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

object renaming {
  /** Snake case mapping */
  final val snakeCase: String => String = _.replaceAll(
    "([A-Z]+)([A-Z][a-z])",
    "$1_$2"
  ).replaceAll("([a-z\\d])([A-Z])", "$1_$2").toLowerCase

  /** Kebab case mapping */
  val kebabCase: String => String =
    _.replaceAll(
      "([A-Z]+)([A-Z][a-z])",
      "$1-$2"
    ).replaceAll("([a-z\\d])([A-Z])", "$1-$2").toLowerCase

  final def replaceWith(pairs: (String, String)*): String => String =
    original => pairs.toMap.getOrElse(original, original)
}