package io.circe

import cats.data.Xor
import java.time.LocalDateTime
import java.time.format.{ DateTimeFormatter, DateTimeParseException }

package object java8 {
  final def decodeLocalDateTime(formatter: DateTimeFormatter): Decoder[LocalDateTime] =
    Decoder.instance { c =>
      c.as[String].flatMap { s =>
        try Xor.right(LocalDateTime.parse(s, formatter)) catch {
          case _: DateTimeParseException => Xor.left(DecodingFailure("LocalDateTime", c.history))
        }
      }
    }

  final def encodeLocalDateTime(formatter: DateTimeFormatter): Encoder[LocalDateTime] =
    Encoder.instance(time => Json.string(time.format(formatter)))

  implicit final val decodeLocalDateTimeDefault: Decoder[LocalDateTime] =
    decodeLocalDateTime(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

  implicit final val encodeLocalDateTimeDefault: Encoder[LocalDateTime] =
    encodeLocalDateTime(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
}
