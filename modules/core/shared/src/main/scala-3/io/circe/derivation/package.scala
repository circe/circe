package io.circe.derivation

import scala.compiletime.{constValue, erasedValue, summonFrom}
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

inline final def deriveDecoder[A](
  transformNames: String => String = Predef.identity,
  useDefaults: Boolean = true,
  discriminator: Option[String] = None
)(using inline A: Mirror.Of[A]): Decoder[A] =
  given Configuration = Configuration(transformNames, useDefaults, discriminator)
  Decoder.derived[A]

inline final def deriveEncoder[A](
  transformNames: String => String = Predef.identity,
  discriminator: Option[String] = None
)(using inline A: Mirror.Of[A]): Encoder.AsObject[A] =
  given Configuration = Configuration(transformNames, useDefaults = true, discriminator)
  Encoder.AsObject.derived[A]

inline final def deriveCodec[A](
  transformNames: String => String = Predef.identity,
  useDefaults: Boolean = true,
  discriminator: Option[String] = None
)(using inline A: Mirror.Of[A]): Codec.AsObject[A] =
  given Configuration = Configuration(transformNames, useDefaults, discriminator)
  Codec.AsObject.derived[A]
