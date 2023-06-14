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
    val typeTests = summonTypeTestRecursively[mirror.MirroredElemTypes, A]
    val labels = summonLabelsRecursively[mirror.MirroredElemTypes].toArray.map(conf.transformConstructorNames)

    def findIndex(a: A): Int =
      def loop(n: Int): Int = 
        if n >= typeTests.length then mirror.ordinal(a) // well this should never happen :|
        else if typeTests(n).unapply(a).isDefined then n
        else loop(n + 1)
      loop(0)
    new ConfiguredEnumEncoder[A]:
      def apply(a: A): Json =
        val index = findIndex(a)
        Json.fromString(labels(index))

  inline final def derive[A: Mirror.SumOf](
    transformConstructorNames: String => String = Configuration.default.transformConstructorNames
  ): Encoder[A] =
    derived[A](using Configuration.default.withTransformConstructorNames(transformConstructorNames))
