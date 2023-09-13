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

package io.circe

private[circe] trait LiteralEncoders {
  private[this] final class LiteralEncoder[L](private[this] final val encoded: Json) extends Encoder[L] {
    final def apply(a: L): Json = encoded
  }

  /**
   * Encode a `String` whose value is known at compile time.
   *
   * @group Literal
   */
  implicit final def encodeLiteralString[L <: String](implicit L: ValueOf[L]): Encoder[L] =
    new LiteralEncoder[L](Encoder.encodeString(L.value))

  /**
   * Encode a `Double` whose value is known at compile time.
   *
   * @group Literal
   */
  implicit final def encodeLiteralDouble[L <: Double](implicit L: ValueOf[L]): Encoder[L] =
    new LiteralEncoder[L](Encoder.encodeDouble(L.value))

  /**
   * Encode a `Float` whose value is known at compile time.
   *
   * @group Literal
   */
  implicit final def encodeLiteralFloat[L <: Float](implicit L: ValueOf[L]): Encoder[L] =
    new LiteralEncoder[L](Encoder.encodeFloat(L.value))

  /**
   * Encode a `Long` whose value is known at compile time.
   *
   * @group Literal
   */
  implicit final def encodeLiteralLong[L <: Long](implicit L: ValueOf[L]): Encoder[L] =
    new LiteralEncoder[L](Encoder.encodeLong(L.value))

  /**
   * Encode a `Int` whose value is known at compile time.
   *
   * @group Literal
   */
  implicit final def encodeLiteralInt[L <: Int](implicit L: ValueOf[L]): Encoder[L] =
    new LiteralEncoder[L](Encoder.encodeInt(L.value))

  /**
   * Encode a `Char` whose value is known at compile time.
   *
   * @group Literal
   */
  implicit final def encodeLiteralChar[L <: Char](implicit L: ValueOf[L]): Encoder[L] =
    new LiteralEncoder[L](Encoder.encodeChar(L.value))

  /**
   * Encode a `Boolean` whose value is known at compile time.
   *
   * @group Literal
   */
  implicit final def encodeLiteralBoolean[L <: Boolean](implicit L: ValueOf[L]): Encoder[L] =
    new LiteralEncoder[L](Encoder.encodeBoolean(L.value))
}
