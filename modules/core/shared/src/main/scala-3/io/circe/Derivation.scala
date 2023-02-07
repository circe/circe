package io.circe

import scala.compiletime.constValue
import scala.deriving.Mirror
import Predef.genericArrayOps
import cats.data.{ NonEmptyList, Validated }
import io.circe.derivation._

@deprecated(since = "0.14.4")
object Derivation {
  inline final def summonLabels[T <: Tuple]: Array[String] = summonLabelsRec[T].toArray
  inline final def summonDecoders[T <: Tuple]: Array[Decoder[_]] =
    derivation.summonDecoders[T](using Configuration.default).toArray
  inline final def summonEncoders[T <: Tuple]: Array[Encoder[_]] =
    derivation.summonEncoders[T](using Configuration.default).toArray

  inline final def summonEncoder[A]: Encoder[A] = derivation.summonEncoder[A](using Configuration.default)
  inline final def summonDecoder[A]: Decoder[A] = derivation.summonDecoder[A](using Configuration.default)

  inline final def summonLabelsRec[T <: Tuple]: List[String] = derivation.summonLabels[T]
  inline final def summonDecodersRec[T <: Tuple]: List[Decoder[_]] =
    derivation.summonDecoders[T](using Configuration.default)
  inline final def summonEncodersRec[T <: Tuple]: List[Encoder[_]] =
    derivation.summonEncoders[T](using Configuration.default)
}

@deprecated(since = "0.14.4")
private[circe] trait DerivedInstance
private[circe] trait DerivedEncoder
private[circe] trait DerivedDecoder

private[circe] trait EncoderDerivation:
  inline final def derived[A](using inline A: Mirror.Of[A]): Encoder.AsObject[A] =
    ConfiguredEncoder.derived[A](using Configuration.default)
  inline final def derivedConfigured[A](using
    inline A: Mirror.Of[A],
    inline configuration: Configuration
  ): Encoder.AsObject[A] =
    ConfiguredEncoder.derived[A]

private[circe] trait DecoderDerivation:
  inline final def derived[A](using inline A: Mirror.Of[A]): Decoder[A] =
    ConfiguredDecoder.derived[A](using Configuration.default)
  inline final def derivedConfigured[A](using inline A: Mirror.Of[A], inline configuration: Configuration): Decoder[A] =
    ConfiguredDecoder.derived[A]

private[circe] trait CodecDerivation:
  inline final def derived[A](using inline A: Mirror.Of[A]): Codec.AsObject[A] =
    ConfiguredCodec.derived[A](using Configuration.default)
  inline final def derivedConfigured[A](using
    inline A: Mirror.Of[A],
    inline configuration: Configuration
  ): Codec.AsObject[A] =
    ConfiguredCodec.derived[A]
