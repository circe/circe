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

package object literal {
  implicit final class JsonStringContext(sc: StringContext) {
    final def json(args: Any*): Json = macro JsonLiteralMacros.jsonStringContext
  }

  implicit final def decodeLiteralString[S <: String]: Decoder[S] = macro LiteralInstanceMacros.decodeLiteralString[S]
  implicit final def decodeLiteralDouble[S <: Double]: Decoder[S] = macro LiteralInstanceMacros.decodeLiteralDouble[S]
  implicit final def decodeLiteralFloat[S <: Float]: Decoder[S] = macro LiteralInstanceMacros.decodeLiteralFloat[S]
  implicit final def decodeLiteralLong[S <: Long]: Decoder[S] = macro LiteralInstanceMacros.decodeLiteralLong[S]
  implicit final def decodeLiteralInt[S <: Int]: Decoder[S] = macro LiteralInstanceMacros.decodeLiteralInt[S]
  implicit final def decodeLiteralChar[S <: Char]: Decoder[S] = macro LiteralInstanceMacros.decodeLiteralChar[S]
  implicit final def decodeLiteralBoolean[S <: Boolean]: Decoder[S] =
    macro LiteralInstanceMacros.decodeLiteralBoolean[S]

  implicit final def encodeLiteralString[S <: String]: Encoder[S] = macro LiteralInstanceMacros.encodeLiteralString[S]
  implicit final def encodeLiteralDouble[S <: Double]: Encoder[S] = macro LiteralInstanceMacros.encodeLiteralDouble[S]
  implicit final def encodeLiteralFloat[S <: Float]: Encoder[S] = macro LiteralInstanceMacros.encodeLiteralFloat[S]
  implicit final def encodeLiteralLong[S <: Long]: Encoder[S] = macro LiteralInstanceMacros.encodeLiteralLong[S]
  implicit final def encodeLiteralInt[S <: Int]: Encoder[S] = macro LiteralInstanceMacros.encodeLiteralInt[S]
  implicit final def encodeLiteralChar[S <: Char]: Encoder[S] = macro LiteralInstanceMacros.encodeLiteralChar[S]
  implicit final def encodeLiteralBoolean[S <: Boolean]: Encoder[S] =
    macro LiteralInstanceMacros.encodeLiteralBoolean[S]
}
