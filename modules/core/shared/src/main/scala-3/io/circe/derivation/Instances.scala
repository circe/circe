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

package io.circe
package derivation

import scala.deriving.Mirror

private[circe] sealed trait Decoders[A] extends Serializable {
  def decoders(using conf: Configuration): List[Decoder[?]]
}

object Decoders {
  final class Impl[A](ds: Configuration ?=> List[Decoder[?]]) extends Decoders[A] with Serializable {
    final def decoders(using conf: Configuration): List[Decoder[?]] = ds
  }

  inline given inst[A, ET <: Tuple](using
    inline m: Mirror.Of[A] { type MirroredElemTypes = ET }
  ): Decoders[A] =
    new Impl[A](summonDecoders[ET](inline m match {
      case _: Mirror.SumOf[A]     => true
      case _: Mirror.ProductOf[A] => false
    }))
}

private[circe] sealed trait Encoders[A] extends Serializable {
  def encoders(using conf: Configuration): List[Encoder[?]]
}

object Encoders {
  final class Impl[A](es: Configuration ?=> List[Encoder[?]]) extends Encoders[A] with Serializable {
    final def encoders(using conf: Configuration): List[Encoder[?]] = es
  }

  inline given inst[A, ET <: Tuple](using
    inline m: Mirror.Of[A] { type MirroredElemTypes = ET }
  ): Encoders[A] =
    new Impl[A](summonEncoders[ET](inline m match {
      case _: Mirror.SumOf[A]     => true
      case _: Mirror.ProductOf[A] => false
    }))
}
