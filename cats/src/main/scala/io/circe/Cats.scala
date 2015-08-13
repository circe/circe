package io.circe

import scala.language.implicitConversions

object Cats extends Eqs with Shows with Monads with Encoders with Decoders with Contravariants {

  implicit def toEncoderCompanionOps(encoder: Encoder.type): EncoderCompanionOps =
    new EncoderCompanionOps(encoder)

  implicit def toDecoderOps[A](decoder: Decoder[A]): DecoderOps[A] =
    new DecoderOps(decoder)


  implicit def toJsonObjectOps(jsonObject: JsonObject): JsonObjectOps =
    new JsonObjectOps(jsonObject)

  implicit def toJsonObjectCompanionOps(jsonObject: JsonObject.type): JsonObjectCompanionOps =
    new JsonObjectCompanionOps(jsonObject)


  implicit def toExtraCursorOps(cursor: Cursor): ExtraCursorOperations =
    new ExtraCursorOperations(cursor)

  implicit def toExtraACursorOps(cursor: ACursor): ExtraACursorOperations =
    new ExtraACursorOperations(cursor)

  implicit def toExtraHCursorOps(cursor: HCursor): ExtraHCursorOperations =
    new ExtraHCursorOperations(cursor)

}
