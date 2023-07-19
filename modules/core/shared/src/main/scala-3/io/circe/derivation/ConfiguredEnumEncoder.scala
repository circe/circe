/*
 * Copyright 2023 circe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
