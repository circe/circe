/*
 * Copyright 2024 circe
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

package io.circe.generic

import io.circe.{ Decoder, Encoder }
import io.circe.`export`.Exported
import scala.deriving.Mirror

/**
 * Fully automatic codec derivation.
 *
 * Extending this trait provides [[io.circe.Decoder]] and [[io.circe.Encoder]]
 * instances for case classes (if all members have instances), sealed
 * trait hierarchies, etc.
 */
trait AutoDerivation {
  implicit inline final def deriveDecoder[A](using inline A: Mirror.Of[A]): Exported[Decoder[A]] =
    Exported(Decoder.derived[A])
  implicit inline final def deriveEncoder[A](using inline A: Mirror.Of[A]): Exported[Encoder.AsObject[A]] =
    Exported(Encoder.AsObject.derived[A])
}

object auto extends AutoDerivation
