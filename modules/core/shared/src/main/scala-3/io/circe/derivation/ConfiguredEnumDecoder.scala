package io.circe.derivation

import scala.deriving.Mirror
import scala.compiletime.constValue
import Predef.genericArrayOps
import io.circe.{Decoder, DecodingFailure, HCursor}
import io.circe.syntax._

trait ConfiguredEnumDecoder[A] extends Decoder[A]
object ConfiguredEnumDecoder:
  inline final def derived[A](using conf: EnumConfiguration = EnumConfiguration.default)(using mirror: Mirror.SumOf[A]): ConfiguredEnumDecoder[A] =
    val name = constValue[mirror.MirroredLabel]
    val cases = summonSingletonCases[mirror.MirroredElemTypes, A](name)
    val labels = summonLabels[mirror.MirroredElemLabels].toArray.map(conf.decodeTransformNames)
    new ConfiguredEnumDecoder[A]:
      def apply(c: HCursor): Decoder.Result[A] =
        c.as[String].flatMap { s =>
          labels.indexOf(s) match
            case -1 => Left(DecodingFailure(s"enum $name does not contain case: $s", c.history))
            case index => Right(cases(index))
        }

  inline def derive[R: Mirror.SumOf](decodeTransformNames: String => String = EnumConfiguration.default.decodeTransformNames): Decoder[R] =
    given EnumConfiguration = EnumConfiguration.default.withDecodeTransformNames(decodeTransformNames)
    derived[R]
