package io.circe

import java.time.{ Instant, LocalDate, LocalDateTime, LocalTime, OffsetDateTime, ZonedDateTime }
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.{
  ISO_LOCAL_DATE,
  ISO_LOCAL_DATE_TIME,
  ISO_LOCAL_TIME,
  ISO_OFFSET_DATE_TIME,
  ISO_ZONED_DATE_TIME
}

private[circe] trait JavaTimeEncoders {
  implicit final val encodeInstant: Encoder[Instant] = Encoder.instance(time => Json.fromString(time.toString))

  final def encodeLocalDateTime(formatter: DateTimeFormatter): Encoder[LocalDateTime] =
    Encoder.instance(time => Json.fromString(time.format(formatter)))

  implicit final val encodeLocalDateTimeDefault: Encoder[LocalDateTime] = encodeLocalDateTime(ISO_LOCAL_DATE_TIME)

  final def encodeZonedDateTime(formatter: DateTimeFormatter): Encoder[ZonedDateTime] =
    Encoder.instance(time => Json.fromString(time.format(formatter)))

  implicit final val encodeZonedDateTimeDefault: Encoder[ZonedDateTime] = encodeZonedDateTime(ISO_ZONED_DATE_TIME)

  final def encodeOffsetDateTime(formatter: DateTimeFormatter): Encoder[OffsetDateTime] =
    Encoder.instance(time => Json.fromString(time.format(formatter)))

  implicit final val encodeOffsetDateTimeDefault: Encoder[OffsetDateTime] = encodeOffsetDateTime(ISO_OFFSET_DATE_TIME)

  final def encodeLocalDate(formatter: DateTimeFormatter): Encoder[LocalDate] =
    Encoder.instance(time => Json.fromString(time.format(formatter)))

  implicit final val encodeLocalDateDefault: Encoder[LocalDate] = encodeLocalDate(ISO_LOCAL_DATE)

  final def encodeLocalTime(formatter: DateTimeFormatter): Encoder[LocalTime] =
    Encoder.instance(time => Json.fromString(time.format(formatter)))

  implicit final val encodeLocalTimeDefault: Encoder[LocalTime] = encodeLocalTime(ISO_LOCAL_TIME)
}
