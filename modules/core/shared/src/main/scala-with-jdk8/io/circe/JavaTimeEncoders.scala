package io.circe

import java.time.{
  Duration,
  Instant,
  LocalDate,
  LocalDateTime,
  LocalTime,
  OffsetDateTime,
  OffsetTime,
  Period,
  YearMonth,
  ZonedDateTime,
  ZoneId
}
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.{
  ISO_LOCAL_DATE,
  ISO_LOCAL_DATE_TIME,
  ISO_LOCAL_TIME,
  ISO_OFFSET_DATE_TIME,
  ISO_OFFSET_TIME,
  ISO_ZONED_DATE_TIME
}

private[circe] trait JavaTimeEncoders {
  /**
   * @group Time
   */
  implicit final val encodeDuration: Encoder[Duration] = Encoder.instance(duration =>
    Json.fromString(duration.toString)
  )

  /**
   * @group Time
   */
  implicit final val encodeInstant: Encoder[Instant] = Encoder.instance(time =>
    Json.fromString(time.toString)
  )

  /**
   * @group Time
   */
  implicit final val encodePeriod: Encoder[Period] = Encoder.instance(period =>
    Json.fromString(period.toString)
  )

  /**
   * @group Time
   */
  implicit final val encodeZoneId: Encoder[ZoneId] = Encoder.instance(zoneId =>
    Json.fromString(zoneId.getId)
  )

  /**
   * @group Time
   */
  final def encodeLocalDate(formatter: DateTimeFormatter): Encoder[LocalDate] =
    Encoder.instance(time => Json.fromString(time.format(formatter)))

  /**
   * @group Time
   */
  final def encodeLocalTime(formatter: DateTimeFormatter): Encoder[LocalTime] =
    Encoder.instance(time => Json.fromString(time.format(formatter)))

  /**
   * @group Time
   */
  final def encodeLocalDateTime(formatter: DateTimeFormatter): Encoder[LocalDateTime] =
    Encoder.instance(time => Json.fromString(time.format(formatter)))

  /**
   * @group Time
   */
  final def encodeOffsetTime(formatter: DateTimeFormatter): Encoder[OffsetTime] =
    Encoder.instance(time => Json.fromString(time.format(formatter)))

  /**
   * @group Time
   */
  final def encodeOffsetDateTime(formatter: DateTimeFormatter): Encoder[OffsetDateTime] =
    Encoder.instance(time => Json.fromString(time.format(formatter)))

  /**
   * @group Time
   */
  final def encodeZonedDateTime(formatter: DateTimeFormatter): Encoder[ZonedDateTime] =
    Encoder.instance(time => Json.fromString(time.format(formatter)))

  /**
   * @group Time
   */
  final def encodeYearMonth(formatter: DateTimeFormatter): Encoder[YearMonth] =
    Encoder.instance(time => Json.fromString(time.format(formatter)))

  /**
   * @group Time
   */
  implicit final def encodeLocalDateDefault: Encoder[LocalDate] = encodeLocalDate(ISO_LOCAL_DATE)

  /**
   * @group Time
   */
  implicit final def encodeLocalTimeDefault: Encoder[LocalTime] = encodeLocalTime(ISO_LOCAL_TIME)

  /**
   * @group Time
   */
  implicit final def encodeLocalDateTimeDefault: Encoder[LocalDateTime] = encodeLocalDateTime(ISO_LOCAL_DATE_TIME)

  /**
   * @group Time
   */
  implicit final def encodeOffsetTimeDefault: Encoder[OffsetTime] = encodeOffsetTime(ISO_OFFSET_TIME)

  /**
   * @group Time
   */
  implicit final def encodeOffsetDateTimeDefault: Encoder[OffsetDateTime] = encodeOffsetDateTime(ISO_OFFSET_DATE_TIME)

  /**
   * @group Time
   */
  implicit final def encodeZonedDateTimeDefault: Encoder[ZonedDateTime] = encodeZonedDateTime(ISO_ZONED_DATE_TIME)

  /**
   * @group Time
   */
  implicit final def encodeYearMonthDefault: Encoder[YearMonth] =
    encodeYearMonth(DateTimeFormatter.ofPattern("yyyy-MM"))
}
