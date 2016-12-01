package io.circe

import java.time.{ Instant, LocalDate, LocalDateTime, LocalTime, OffsetDateTime, ZonedDateTime }
import java.time.format.{ DateTimeFormatter, DateTimeParseException }
import java.time.format.DateTimeFormatter.{
  ISO_LOCAL_DATE,
  ISO_LOCAL_DATE_TIME,
  ISO_LOCAL_TIME,
  ISO_OFFSET_DATE_TIME,
  ISO_ZONED_DATE_TIME
}

private[circe] trait JavaTimeDecoders {
  implicit final val decodeInstant: Decoder[Instant] =
    Decoder.instance { c =>
      c.as[String] match {
        case Right(s) => try Right(Instant.parse(s)) catch {
          case _: DateTimeParseException => Left(DecodingFailure("Instant", c.history))
        }
        case l @ Left(_) => l.asInstanceOf[Decoder.Result[Instant]]
      }
    }

  final def decodeLocalDateTime(formatter: DateTimeFormatter): Decoder[LocalDateTime] =
    Decoder.instance { c =>
      c.as[String] match {
        case Right(s) => try Right(LocalDateTime.parse(s, formatter)) catch {
          case _: DateTimeParseException => Left(DecodingFailure("LocalDateTime", c.history))
        }
        case l @ Left(_) => l.asInstanceOf[Decoder.Result[LocalDateTime]]
      }
    }

  implicit final val decodeLocalDateTimeDefault: Decoder[LocalDateTime] = decodeLocalDateTime(ISO_LOCAL_DATE_TIME)

  final def decodeZonedDateTime(formatter: DateTimeFormatter): Decoder[ZonedDateTime] =
    Decoder.instance { c =>
      c.as[String] match {
        case Right(s) => try Right(ZonedDateTime.parse(s, formatter)) catch {
          case _: DateTimeParseException => Left(DecodingFailure("ZonedDateTime", c.history))
        }
        case l @ Left(_) => l.asInstanceOf[Decoder.Result[ZonedDateTime]]
      }
    }

  implicit final val decodeZonedDateTimeDefault: Decoder[ZonedDateTime] = decodeZonedDateTime(ISO_ZONED_DATE_TIME)

  final def decodeOffsetDateTime(formatter: DateTimeFormatter): Decoder[OffsetDateTime] =
    Decoder.instance { c =>
      c.as[String] match {
        case Right(s) => try Right(OffsetDateTime.parse(s, formatter)) catch {
          case _: DateTimeParseException => Left(DecodingFailure("OffsetDateTime", c.history))
        }
        case l @ Left(_) => l.asInstanceOf[Decoder.Result[OffsetDateTime]]
      }
    }

  implicit final val decodeOffsetDateTimeDefault: Decoder[OffsetDateTime] = decodeOffsetDateTime(ISO_OFFSET_DATE_TIME)

  final def decodeLocalDate(formatter: DateTimeFormatter): Decoder[LocalDate] =
    Decoder.instance { c =>
      c.as[String] match {
        case Right(s) => try Right(LocalDate.parse(s, formatter)) catch {
          case _: DateTimeParseException => Left(DecodingFailure("LocalDate", c.history))
        }
        case l @ Left(_) => l.asInstanceOf[Decoder.Result[LocalDate]]
      }
    }

  implicit final val decodeLocalDateDefault: Decoder[LocalDate] = decodeLocalDate(ISO_LOCAL_DATE)

  final def decodeLocalTime(formatter: DateTimeFormatter): Decoder[LocalTime] =
    Decoder.instance { c =>
      c.as[String] match {
        case Right(s) => try Right(LocalTime.parse(s, formatter)) catch {
          case _: DateTimeParseException => Left(DecodingFailure("LocalTime", c.history))
        }
        case l @ Left(_) => l.asInstanceOf[Decoder.Result[LocalTime]]
      }
    }

  implicit final val decodeLocalTimeDefault: Decoder[LocalTime] = decodeLocalTime(ISO_LOCAL_TIME)
}
