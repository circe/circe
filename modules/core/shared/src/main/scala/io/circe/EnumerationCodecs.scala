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
  final def codecForEnumeration[E <: Enumeration](enumeration: E): Codec[E#Value] = new Codec[E#Value] {
    final def apply(c: HCursor): Decoder.Result[E#Value] = Decoder.decodeString(c).flatMap { str =>
      Try(enumeration.withName(str)) match {
        case Success(a) => Right(a)
        case Failure(t) =>
          Left(
            DecodingFailure(
              s"Couldn't decode value '$str'. " +
                s"Allowed values: '${enumeration.values.mkString(",")}'",
              c.history
            )
          )
      }
    }
    final def apply(e: E#Value): Json = Encoder.encodeString(e.toString)
  }
}
