package io.circe

import scala.language.experimental.macros

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
