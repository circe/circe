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
import java.time.temporal.TemporalAccessor

private[circe] abstract class JavaTimeEncoder[A <: TemporalAccessor] extends Encoder[A] {
  protected def format: DateTimeFormatter

  final def apply(a: A): Json = Json.fromString(format.format(a))
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
    new JavaTimeEncoder[LocalDate] {
      protected final def format: DateTimeFormatter = formatter
    }

  /**
   * @group Time
   */
  final def encodeLocalTime(formatter: DateTimeFormatter): Encoder[LocalTime] =
    new JavaTimeEncoder[LocalTime] {
      protected final def format: DateTimeFormatter = formatter
    }

  /**
   * @group Time
   */
  final def encodeLocalDateTime(formatter: DateTimeFormatter): Encoder[LocalDateTime] =
    new JavaTimeEncoder[LocalDateTime] {
      protected final def format: DateTimeFormatter = formatter
    }

  /**
   * @group Time
   */
  final def encodeOffsetTime(formatter: DateTimeFormatter): Encoder[OffsetTime] =
    new JavaTimeEncoder[OffsetTime] {
      protected final def format: DateTimeFormatter = formatter
    }

  /**
   * @group Time
   */
  final def encodeOffsetDateTime(formatter: DateTimeFormatter): Encoder[OffsetDateTime] =
    new JavaTimeEncoder[OffsetDateTime] {
      protected final def format: DateTimeFormatter = formatter
    }

  /**
   * @group Time
   */
  final def encodeZonedDateTime(formatter: DateTimeFormatter): Encoder[ZonedDateTime] =
    new JavaTimeEncoder[ZonedDateTime] {
      protected final def format: DateTimeFormatter = formatter
    }

  /**
   * @group Time
   */
  final def encodeYearMonth(formatter: DateTimeFormatter): Encoder[YearMonth] =
    new JavaTimeEncoder[YearMonth] {
      protected final def format: DateTimeFormatter = formatter
    }

  /**
   * @group Time
   */
  implicit final val encodeLocalDateDefault: Encoder[LocalDate] =
    new JavaTimeEncoder[LocalDate] {
      protected final def format: DateTimeFormatter = ISO_LOCAL_DATE
    }

  /**
   * @group Time
   */
  implicit final val encodeLocalTimeDefault: Encoder[LocalTime] =
    new JavaTimeEncoder[LocalTime] {
      protected final def format: DateTimeFormatter = ISO_LOCAL_TIME
    }

  /**
   * @group Time
   */
  implicit final val encodeLocalDateTimeDefault: Encoder[LocalDateTime] =
    new JavaTimeEncoder[LocalDateTime] {
      protected final def format: DateTimeFormatter = ISO_LOCAL_DATE_TIME
    }

  /**
   * @group Time
   */
  implicit final val encodeOffsetTimeDefault: Encoder[OffsetTime] =
    new JavaTimeEncoder[OffsetTime] {
      protected final def format: DateTimeFormatter = ISO_OFFSET_TIME
    }

  /**
   * @group Time
   */
  implicit final val encodeOffsetDateTimeDefault: Encoder[OffsetDateTime] =
    new JavaTimeEncoder[OffsetDateTime] {
      protected final def format: DateTimeFormatter = ISO_OFFSET_DATE_TIME
    }

  /**
   * @group Time
   */
  implicit final val encodeZonedDateTimeDefault: Encoder[ZonedDateTime] =
    new JavaTimeEncoder[ZonedDateTime] {
      protected final def format: DateTimeFormatter = ISO_ZONED_DATE_TIME
    }

  /**
   * @group Time
   */
  implicit final val encodeYearMonthDefault: Encoder[YearMonth] =
    new JavaTimeEncoder[YearMonth] {
      protected final def format: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
    }
}
