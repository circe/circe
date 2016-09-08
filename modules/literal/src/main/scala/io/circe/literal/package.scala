package io.circe

import scala.language.experimental.macros

package object literal {
  implicit final class JsonStringContext(sc: StringContext) {
    final def json(args: Any*): Json = macro LiteralMacros.jsonStringContext
  }

  implicit final def decodeLiteralString[S <: String]: Decoder[S] =
    macro LiteralMacros.decodeLiteralStringImpl[S]

  implicit final def decodeLiteralBoolean[S <: Boolean]: Decoder[S] =
    macro LiteralMacros.decodeLiteralBooleanImpl[S]

  implicit final def decodeLiteralDouble[S <: Double]: Decoder[S] =
    macro LiteralMacros.decodeLiteralDoubleImpl[S]

  implicit final def decodeLiteralFloat[S <: Float]: Decoder[S] =
    macro LiteralMacros.decodeLiteralFloatImpl[S]

  implicit final def decodeLiteralLong[S <: Long]: Decoder[S] =
    macro LiteralMacros.decodeLiteralLongImpl[S]

  implicit final def decodeLiteralInt[S <: Int]: Decoder[S] =
    macro LiteralMacros.decodeLiteralIntImpl[S]

  implicit final def decodeLiteralChar[S <: Char]: Decoder[S] =
    macro LiteralMacros.decodeLiteralCharImpl[S]

  implicit final def encodeLiteralString[S <: String]: Encoder[S] =
    macro LiteralMacros.encodeLiteralStringImpl[S]

  implicit final def encodeLiteralBoolean[S <: Boolean]: Encoder[S] =
    macro LiteralMacros.encodeLiteralBooleanImpl[S]

  implicit final def encodeLiteralDouble[S <: Double]: Encoder[S] =
    macro LiteralMacros.encodeLiteralDoubleImpl[S]

  implicit final def encodeLiteralFloat[S <: Float]: Encoder[S] =
    macro LiteralMacros.encodeLiteralFloatImpl[S]

  implicit final def encodeLiteralLong[S <: Long]: Encoder[S] =
    macro LiteralMacros.encodeLiteralLongImpl[S]

  implicit final def encodeLiteralInt[S <: Int]: Encoder[S] =
    macro LiteralMacros.encodeLiteralIntImpl[S]

  implicit final def encodeLiteralChar[S <: Char]: Encoder[S] =
    macro LiteralMacros.encodeLiteralCharImpl[S]
}
