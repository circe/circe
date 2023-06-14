package io.circe.derivation

import scala.deriving.Mirror
import scala.compiletime.constValue
import Predef.genericArrayOps
import io.circe.{ Decoder, DecodingFailure, HCursor }
import scala.collection.mutable.ArraySeq

trait ConfiguredEnumDecoder[A] extends Decoder[A]
object ConfiguredEnumDecoder:
  inline final def derived[A](using conf: Configuration)(using mirror: Mirror.SumOf[A]): ConfiguredEnumDecoder[A] =
    val cases = summonSingletonCases[mirror.MirroredElemTypes, A](constValue[mirror.MirroredLabel]).toVector
    val labels = summonLabelsRecursively[mirror.MirroredElemTypes].toArray.map(conf.transformConstructorNames)
    new ConfiguredEnumDecoder[A]:
      def apply(c: HCursor): Decoder.Result[A] =
        c.as[String].flatMap { caseName =>
          labels.indexOf(caseName) match
            case -1 =>
              Left(
                DecodingFailure(s"enum ${constValue[mirror.MirroredLabel]} does not contain case: $caseName", c.history)
              )
            case index => Right(cases(index))
        }

  inline final def derive[R: Mirror.SumOf](
    transformConstructorNames: String => String = Configuration.default.transformConstructorNames
  ): Decoder[R] =
    derived[R](using Configuration.default.withTransformConstructorNames(transformConstructorNames))
