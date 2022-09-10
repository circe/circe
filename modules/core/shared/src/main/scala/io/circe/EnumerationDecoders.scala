package io.circe

import scala.util.{ Failure, Success, Try }

private[circe] trait EnumerationDecoders {

  /**
   * {{{
   *   object WeekDay extends Enumeration { ... }
   *   implicit val weekDayDecoder = Decoder.decodeEnumeration(WeekDay)
   * }}}
   *
   * @group Utilities
   */
  final def decodeEnumeration[E <: Enumeration](enumeration: E): Decoder[E#Value] = new Decoder[E#Value] {
    final def apply(c: HCursor): Decoder.Result[E#Value] = Decoder.decodeString(c).flatMap { str =>
      Try(enumeration.withName(str)) match {
        case Success(a) => Right(a)
        case Failure(_) =>
          Left(
            DecodingFailure(
              s"Couldn't decode value '$str'. " +
                s"Allowed values: '${enumeration.values.mkString(",")}'",
              c.history
            )
          )
      }
    }
  }

}
