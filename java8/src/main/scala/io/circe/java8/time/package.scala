package io.circe.java8

import cats.data.Xor
import io.circe.{ Decoder, DecodingFailure, Encoder, Json }
import java.time.{ Instant, LocalDate, LocalDateTime, OffsetDateTime, ZonedDateTime }
import java.time.format.{ DateTimeFormatter, DateTimeParseException }
import java.time.format.DateTimeFormatter.{
  ISO_LOCAL_DATE,
  ISO_LOCAL_DATE_TIME,
  ISO_OFFSET_DATE_TIME,
  ISO_ZONED_DATE_TIME
}

package object time {
  implicit final val decodeInstant: Decoder[Instant] =
    Decoder.instance { c =>
      c.as[String].flatMap { s =>
        try Xor.right(Instant.parse(s)) catch {
          case _: DateTimeParseException => Xor.left(DecodingFailure("Instant", c.history))
        }
      }
    }

  implicit final val encodeInstant: Encoder[Instant] = Encoder.instance(time => Json.fromString(time.toString))

  final def decodeLocalDateTime(formatter: DateTimeFormatter): Decoder[LocalDateTime] =
    Decoder.instance { c =>
      c.as[String].flatMap { s =>
        try Xor.right(LocalDateTime.parse(s, formatter)) catch {
          case _: DateTimeParseException => Xor.left(DecodingFailure("LocalDateTime", c.history))
        }
      }
    }

  final def encodeLocalDateTime(formatter: DateTimeFormatter): Encoder[LocalDateTime] =
    Encoder.instance(time => Json.fromString(time.format(formatter)))

  implicit final val decodeLocalDateTimeDefault: Decoder[LocalDateTime] = decodeLocalDateTime(ISO_LOCAL_DATE_TIME)
  implicit final val encodeLocalDateTimeDefault: Encoder[LocalDateTime] = encodeLocalDateTime(ISO_LOCAL_DATE_TIME)

  final def decodeZonedDateTime(formatter: DateTimeFormatter): Decoder[ZonedDateTime] =
    Decoder.instance { c =>
      c.as[String].flatMap { s =>
        try Xor.right(ZonedDateTime.parse(s, formatter)) catch {
          case _: DateTimeParseException => Xor.left(DecodingFailure("ZonedDateTime", c.history))
        }
      }
    }

  final def encodeZonedDateTime(formatter: DateTimeFormatter): Encoder[ZonedDateTime] =
    Encoder.instance(time => Json.fromString(time.format(formatter)))

  implicit final val decodeZonedDateTimeDefault: Decoder[ZonedDateTime] = decodeZonedDateTime(ISO_ZONED_DATE_TIME)
  implicit final val encodeZonedDateTimeDefault: Encoder[ZonedDateTime] = encodeZonedDateTime(ISO_ZONED_DATE_TIME)

  final def decodeOffsetDateTime(formatter: DateTimeFormatter): Decoder[OffsetDateTime] =
    Decoder.instance { c =>
      c.as[String].flatMap { s =>
        try Xor.right(OffsetDateTime.parse(s, formatter)) catch {
          case _: DateTimeParseException => Xor.left(DecodingFailure("OffsetDateTime", c.history))
        }
      }
    }

  final def encodeOffsetDateTime(formatter: DateTimeFormatter): Encoder[OffsetDateTime] =
    Encoder.instance(time => Json.fromString(time.format(formatter)))

  implicit final val decodeOffsetDateTimeDefault: Decoder[OffsetDateTime] = decodeOffsetDateTime(ISO_OFFSET_DATE_TIME)
  implicit final val encodeOffsetDateTimeDefault: Encoder[OffsetDateTime] = encodeOffsetDateTime(ISO_OFFSET_DATE_TIME)

  final def decodeLocalDate(formatter: DateTimeFormatter): Decoder[LocalDate] =
    Decoder.instance { c =>
      c.as[String].flatMap { s =>
        try Xor.right(LocalDate.parse(s, formatter)) catch {
          case _: DateTimeParseException => Xor.left(DecodingFailure("LocalDate", c.history))
        }
      }
    }

  final def encodeLocalDate(formatter: DateTimeFormatter): Encoder[LocalDate] =
    Encoder.instance(time => Json.fromString(time.format(formatter)))

  implicit final val decodeLocalDateDefault: Decoder[LocalDate] = decodeLocalDate(ISO_LOCAL_DATE)
  implicit final val encodeLocalDateDefault: Encoder[LocalDate] = encodeLocalDate(ISO_LOCAL_DATE)
}
