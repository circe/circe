package io.circe.derivation

import scala.deriving.Mirror
import scala.compiletime.constValue
import Predef.genericArrayOps
import io.circe.{Decoder, DecodingFailure, HCursor}

trait ConfiguredEnumDecoder[A] extends Decoder[A]
object ConfiguredEnumDecoder:
  inline final def derived[A](using conf: EnumConfiguration)(using mirror: Mirror.SumOf[A]): ConfiguredEnumDecoder[A] =
    val name = constValue[mirror.MirroredLabel]
    val cases = summonSingletonCases[mirror.MirroredElemTypes, A](name)
    val labels = summonLabels[mirror.MirroredElemLabels].toArray
    new ConfiguredEnumDecoder[A]:
      def apply(c: HCursor): Decoder.Result[A] =
        c.as[String].flatMap { caseName =>
          val transformedName = conf.decodeTransformNames(caseName)
          labels.indexOf(transformedName) match
            case -1 => Left(DecodingFailure(s"enum $name does not contain case: $transformedName", c.history))
            case index => Right(cases(index))
        }
  
  inline final def derive[R: Mirror.SumOf](transformNames: String => String = EnumConfiguration.default.decodeTransformNames): Decoder[R] =
    derived[R](using EnumConfiguration(transformNames, Predef.identity))
