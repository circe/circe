package io.circe

import scala.compiletime.constValue
import scala.deriving.Mirror
import Predef.genericArrayOps
import cats.data.{ NonEmptyList, Validated }
import io.circe.derivation._

private[circe] trait EncoderDerivation:
  inline final def derived[A](using inline A: Mirror.Of[A]): Encoder.AsObject[A] =
    ConfiguredEncoder.derived[A](using Configuration.default)

private[circe] trait DecoderDerivation:
  inline final def derived[A](using inline A: Mirror.Of[A]): Decoder[A] =
    ConfiguredDecoder.derived[A](using Configuration.default)

private[circe] trait CodecDerivation:
  inline final def derived[A](using inline A: Mirror.Of[A]): Codec.AsObject[A] =
    ConfiguredCodec.derived[A](using Configuration.default)
