package io.circe.derivation

import scala.deriving.Mirror
import scala.compiletime.constValue
import Predef.genericArrayOps
import io.circe.{Decoder, DecodingFailure, Codec, Encoder, HCursor, Json}
import io.circe.syntax._

trait EnumEncoder[A] extends Encoder[A]
object EnumEncoder {
  inline final def derived[A](using m: Mirror.SumOf[A], conf: EnumConfiguration = EnumConfiguration.default): EnumEncoder[A] = {
    // Only used to validate if all cases are singletons
    summonSingletonCases[m.MirroredElemTypes, A](constValue[m.MirroredLabel])
    val labels = summonLabels[m.MirroredElemLabels].toArray.map(conf.encodeTransformNames)
    new EnumEncoder[A] {
      def apply(a: A): Json = labels(m.ordinal(a)).asJson
    }
  }
  
  inline def derive[A: Mirror.SumOf](encodeTransformNames: String => String = EnumConfiguration.default.encodeTransformNames): Encoder[A] = {
    given EnumConfiguration = EnumConfiguration.default.withDecodeTransformNames(encodeTransformNames)
    derived[A]
  }
}

trait EnumDecoder[A] extends Decoder[A]
object EnumDecoder {
  inline final def derived[A](using m: Mirror.SumOf[A], conf: EnumConfiguration = EnumConfiguration.default): EnumDecoder[A] = {
    val name = constValue[m.MirroredLabel]
    val cases = summonSingletonCases[m.MirroredElemTypes, A](name)
    val labels = summonLabels[m.MirroredElemLabels].toArray.map(conf.decodeTransformNames)
    new EnumDecoder[A] {
      def apply(c: HCursor): Decoder.Result[A] =
        c.as[String].flatMap { s =>
          labels.indexOf(s) match
            case -1 => Left(DecodingFailure(s"enum $name does not contain case: $s", c.history))
            case index => Right(cases(index))
        }
    }
  }

  inline def derive[R: Mirror.SumOf](decodeTransformNames: String => String = EnumConfiguration.default.decodeTransformNames): Decoder[R] = {
    given EnumConfiguration = EnumConfiguration.default.withDecodeTransformNames(decodeTransformNames)
    derived[R]
  }
}

trait EnumCodec[A] extends Codec[A]
object EnumCodec {
  inline final def derived[A](using m: Mirror.SumOf[A], conf: EnumConfiguration = EnumConfiguration.default): EnumCodec[A] = {
    val decoder = EnumDecoder.derived[A]
    val encoder = EnumEncoder.derived[A]
    new EnumCodec[A] {
      override def apply(c: HCursor): Decoder.Result[A] = decoder(c)
      override def apply(a: A): Json = encoder(a)
    }
  }

  inline def derive[R: Mirror.SumOf](
    decoderTransform: String => String = EnumConfiguration.default.decodeTransformNames,
    encoderTransform: String => String = EnumConfiguration.default.encodeTransformNames,
  ): Codec[R] = Codec.from(
    EnumDecoder.derive(decoderTransform),
    EnumEncoder.derive(encoderTransform),
  )
}