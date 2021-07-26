package io.circe.derivation

import scala.deriving.Mirror
import scala.compiletime.constValue
import Predef.genericArrayOps
import io.circe.{Decoder, DecodingFailure, Codec, Encoder, HCursor, Json}
import io.circe.syntax._

trait EnumEncoder[A] extends Encoder[A]
object EnumEncoder {
  inline final def derived[A](using m: Mirror.SumOf[A], conf: Configuration = Configuration.default): EnumEncoder[A] = {
    // Only used to validate if all cases are singletons
    summonSingletonCases[m.MirroredElemTypes, A](constValue[m.MirroredLabel])
    val labels = summonLabels[m.MirroredElemLabels].toArray.map(conf.transformNames)
    new EnumEncoder[A] {
      def apply(a: A): Json = labels(m.ordinal(a)).asJson
    }
  }
  
  inline def derive[A: Mirror.SumOf](transformNames: String => String = Configuration.default.transformNames): Encoder[A] = {
    given Configuration = Configuration(transformNames, useDefaults = false, discriminator = None)
    derived[A]
  }
}

trait EnumDecoder[A] extends Decoder[A]
object EnumDecoder {
  inline final def derived[A](using m: Mirror.SumOf[A], conf: Configuration = Configuration.default): EnumDecoder[A] = {
    val name = constValue[m.MirroredLabel]
    val cases = summonSingletonCases[m.MirroredElemTypes, A](name)
    val labels = summonLabels[m.MirroredElemLabels].toArray.map(conf.transformNames)
    new EnumDecoder[A] {
      def apply(c: HCursor): Decoder.Result[A] =
        c.as[String].flatMap { s =>
          labels.indexOf(s) match
            case -1 => Left(DecodingFailure(s"enum $name does not contain case: $s", c.history))
            case index => Right(cases(index))
        }
    }
  }

  inline def derive[R: Mirror.SumOf](transformNames: String => String = Configuration.default.transformNames): Decoder[R] = {
    given Configuration = Configuration(transformNames, useDefaults = false, discriminator = None)
    derived[R]
  }
}

object EnumCodec {
  inline def derive[R: Mirror.SumOf](
    decoderTransform: String => String = Configuration.default.transformNames,
    encoderTransform: String => String = Configuration.default.transformNames,
  ): Codec[R] = Codec.from(
    EnumDecoder.derive(decoderTransform),
    EnumEncoder.derive(encoderTransform),
  )
}