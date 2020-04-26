package io.circe

import scala.util.{ Failure, Success, Try }

private[circe] trait EnumerationCodecs {

  /**
   * {{{
   *   object WeekDay extends Enumeration { ... }
   *   implicit val weekDayCodec = Codec.codecForEnumeration(WeekDay)
   * }}}
   * @group Utilities
   */
  final def codecForEnumeration[E <: Enumeration, J: JsonFactory](enumeration: E): Codec[E#Value, J] = new Codec[E#Value, J] {
    final def apply(c: HCursor): Decoder.Result[E#Value] = Decoder.decodeString(c).flatMap { str =>
      Try(enumeration.withName(str)) match {
        case Success(a) => Right(a)
        case Failure(t) => Left(DecodingFailure(t.getMessage, c.history))
      }
    }
    final def apply(e: E#Value): J = Encoder.encodeString.apply(e.toString)
  }
}
