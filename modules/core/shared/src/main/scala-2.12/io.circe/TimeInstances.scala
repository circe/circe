package io.circe

import java.time.{
  Duration,
  Instant,
  LocalDate,
  LocalDateTime,
  LocalTime,
  OffsetDateTime,
  Period,
  YearMonth,
  ZonedDateTime
}
import java.time.format.{ DateTimeFormatter, DateTimeParseException }
import java.time.format.DateTimeFormatter.{
  ISO_LOCAL_DATE,
  ISO_LOCAL_DATE_TIME,
  ISO_LOCAL_TIME,
  ISO_OFFSET_DATE_TIME,
  ISO_ZONED_DATE_TIME
}

private[circe] object TimeInstances {
  final val yearMonthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
}

trait TimeDecoders {

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

  implicit final def decodeLocalDateTimeDefault: Decoder[LocalDateTime] = decodeLocalDateTime(ISO_LOCAL_DATE_TIME)

  final def decodeZonedDateTime(formatter: DateTimeFormatter): Decoder[ZonedDateTime] =
    Decoder.instance { c =>
      c.as[String] match {
        case Right(s) => try Right(ZonedDateTime.parse(s, formatter)) catch {
          case _: DateTimeParseException => Left(DecodingFailure("ZonedDateTime", c.history))
        }
        case l @ Left(_) => l.asInstanceOf[Decoder.Result[ZonedDateTime]]
      }
    }

  implicit final def decodeZonedDateTimeDefault: Decoder[ZonedDateTime] = decodeZonedDateTime(ISO_ZONED_DATE_TIME)

  final def decodeOffsetDateTime(formatter: DateTimeFormatter): Decoder[OffsetDateTime] =
    Decoder.instance { c =>
      c.as[String] match {
        case Right(s) => try Right(OffsetDateTime.parse(s, formatter)) catch {
          case _: DateTimeParseException => Left(DecodingFailure("OffsetDateTime", c.history))
        }
        case l @ Left(_) => l.asInstanceOf[Decoder.Result[OffsetDateTime]]
      }
    }

  implicit final def decodeOffsetDateTimeDefault: Decoder[OffsetDateTime] = decodeOffsetDateTime(ISO_OFFSET_DATE_TIME)

  implicit final def decodeLocalDateDefault: Decoder[LocalDate] = decodeLocalDate(ISO_LOCAL_DATE)

  final def decodeLocalTime(formatter: DateTimeFormatter): Decoder[LocalTime] =
    Decoder.instance { c =>
      c.as[String] match {
        case Right(s) => try Right(LocalTime.parse(s, formatter)) catch {
          case _: DateTimeParseException => Left(DecodingFailure("LocalTime", c.history))
        }
        case l @ Left(_) => l.asInstanceOf[Decoder.Result[LocalTime]]
      }
    }

  final def decodeLocalDate(formatter: DateTimeFormatter): Decoder[LocalDate] =
    Decoder.instance { c =>
      c.as[String] match {
        case Right(s) => try Right(LocalDate.parse(s, formatter)) catch {
          case _: DateTimeParseException => Left(DecodingFailure("LocalDate", c.history))
        }
        case l @ Left(_) => l.asInstanceOf[Decoder.Result[LocalDate]]
      }
    }

  implicit final def decodeLocalTimeDefault: Decoder[LocalTime] = decodeLocalTime(ISO_LOCAL_TIME)

  implicit final val decodePeriod: Decoder[Period] = Decoder.instance { c =>
    c.as[String] match {
      case Right(s) => try Right(Period.parse(s)) catch {
        case _: DateTimeParseException => Left(DecodingFailure("Period", c.history))
      }
      case l@Left(_) => l.asInstanceOf[Decoder.Result[Period]]
    }
  }

  final def decodeYearMonth(formatter: DateTimeFormatter): Decoder[YearMonth] =
    Decoder.instance { c =>
      c.as[String] match {
        case Right(s) =>
          try Right(YearMonth.parse(s, formatter))
          catch {
            case _: DateTimeParseException => Left(DecodingFailure("YearMonth", c.history))
          }
        case l @ Left(_) => l.asInstanceOf[Decoder.Result[YearMonth]]
      }
    }

  implicit final def decodeYearMonthDefault: Decoder[YearMonth] = decodeYearMonth(TimeInstances.yearMonthFormatter)

  implicit final val decodeDuration: Decoder[Duration] =
    Decoder.instance { c =>
      c.as[String] match {
        case Right(s) => try Right(Duration.parse(s)) catch {
          case _: DateTimeParseException => Left(DecodingFailure("Duration", c.history))
        }
        case l @ Left(_) => l.asInstanceOf[Decoder.Result[Duration]]
      }
    }
}
trait TimeEncoders {
  implicit final val encodeInstant: Encoder[Instant] = Encoder.instance(time => Json.fromString(time.toString))

  final def encodeLocalDateTime(formatter: DateTimeFormatter): Encoder[LocalDateTime] =
    Encoder.instance(time => Json.fromString(time.format(formatter)))

  implicit final def encodeLocalDateTimeDefault: Encoder[LocalDateTime] = encodeLocalDateTime(ISO_LOCAL_DATE_TIME)

  final def encodeZonedDateTime(formatter: DateTimeFormatter): Encoder[ZonedDateTime] =
    Encoder.instance(time => Json.fromString(time.format(formatter)))

  implicit final def encodeZonedDateTimeDefault: Encoder[ZonedDateTime] = encodeZonedDateTime(ISO_ZONED_DATE_TIME)

  final def encodeOffsetDateTime(formatter: DateTimeFormatter): Encoder[OffsetDateTime] =
    Encoder.instance(time => Json.fromString(time.format(formatter)))

  implicit final def encodeOffsetDateTimeDefault: Encoder[OffsetDateTime] = encodeOffsetDateTime(ISO_OFFSET_DATE_TIME)

  final def encodeLocalDate(formatter: DateTimeFormatter): Encoder[LocalDate] =
    Encoder.instance(time => Json.fromString(time.format(formatter)))

  implicit final def encodeLocalDateDefault: Encoder[LocalDate] = encodeLocalDate(ISO_LOCAL_DATE)

  final def encodeLocalTime(formatter: DateTimeFormatter): Encoder[LocalTime] =
    Encoder.instance(time => Json.fromString(time.format(formatter)))

  implicit final def encodeLocalTimeDefault: Encoder[LocalTime] = encodeLocalTime(ISO_LOCAL_TIME)

  implicit final val encodePeriod: Encoder[Period] = Encoder.instance { period =>
    Json.fromString(period.toString)
  }

  final def encodeYearMonth(formatter: DateTimeFormatter): Encoder[YearMonth] =
    Encoder.instance(time => Json.fromString(time.format(formatter)))

  implicit final def encodeYearMonthDefault: Encoder[YearMonth] = encodeYearMonth(TimeInstances.yearMonthFormatter)

  implicit final val encodeDuration: Encoder[Duration] =
    Encoder.instance(duration => Json.fromString(duration.toString))
}
