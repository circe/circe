package io.circe.derivation

import scala.deriving.Mirror
import scala.compiletime.constValue
import Predef.genericArrayOps
import io.circe.{ Encoder, Json }

trait ConfiguredEnumEncoder[A] extends Encoder[A]
object ConfiguredEnumEncoder:
  inline final def derived[A](using conf: Configuration)(using mirror: Mirror.SumOf[A]): ConfiguredEnumEncoder[A] =
    // Only used to validate if all cases are singletons
    summonSingletonCases[mirror.MirroredElemTypes, A](constValue[mirror.MirroredLabel])
    val labels = summonLabels[mirror.MirroredElemLabels].toArray.map(conf.transformConstructorNames)
    new ConfiguredEnumEncoder[A]:
      def apply(a: A): Json = Json.fromString(labels(mirror.ordinal(a)))

  inline final def derive[A: Mirror.SumOf](
    transformConstructorNames: String => String = Configuration.default.transformConstructorNames
  ): Encoder[A] =
    derived[A](using Configuration.default.withTransformConstructorNames(transformConstructorNames))
