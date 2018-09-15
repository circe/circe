package io.circe.java8.time

import io.circe.{ Decoder, DecodingFailure, HCursor, Json }
import java.time.{
  DateTimeException,
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

private[time] abstract class JavaTimeDecoder[A](name: String) extends Decoder[A] {
  protected def parseUnsafe(input: String): A

  /**
   * Add information from the `DateTimeException` to the `DecodingFailure` error message.
   */
  protected def formatMessage(input: String, message: String): String

  final def apply(c: HCursor): Decoder.Result[A] = c.value match {
    case Json.JString(string) =>
      try Right(parseUnsafe(string)) catch {
        case e: DateTimeException =>
          val message = e.getMessage

          if (message.eq(null)) Left(DecodingFailure(name, c.history)) else {
            val newMessage = formatMessage(string, message)
            Left(DecodingFailure(s"$name ($newMessage)", c.history))
          }
      }
    case _ => Left(DecodingFailure(name, c.history))
  }
}

private[time] abstract class StandardJavaTimeDecoder[A](name: String)
    extends JavaTimeDecoder[A](name) {

  protected final def formatMessage(input: String, message: String): String = message
}

private[time] trait JavaTimeDecoders {
  /**
   * @group Time
   */
  implicit final val decodeDuration: Decoder[Duration] =
    new JavaTimeDecoder[Duration]("Duration") {
      protected final def parseUnsafe(input: String): Duration = Duration.parse(input)

      // For some reason the error message for `Duration` does not contain the
      // input string by default.
      protected final def formatMessage(input: String, message: String): String =
        s"Text '$input' cannot be parsed to a Duration"
    }

  /**
   * @group Time
   */
  implicit final val decodeInstant: Decoder[Instant] =
    new StandardJavaTimeDecoder[Instant]("Instant") {
      protected final def parseUnsafe(input: String): Instant = Instant.parse(input)
    }

  /**
   * @group Time
   */
  implicit final val decodePeriod: Decoder[Period] =
    new JavaTimeDecoder[Period]("Period") {
      protected final def parseUnsafe(input: String): Period = Period.parse(input)

      // For some reason the error message for `Period` does not contain the
      // input string by default.
      protected final def formatMessage(input: String, message: String): String =
        s"Text '$input' cannot be parsed to a Period"
    }

  /**
   * @group Time
   */
  implicit final val decodeZoneId: Decoder[ZoneId] =
    new StandardJavaTimeDecoder[ZoneId]("ZoneId") {
      protected final def parseUnsafe(input: String): ZoneId = ZoneId.of(input)
    }

  /**
   * @group Time
   */
  final def decodeLocalDateWithFormatter(formatter: DateTimeFormatter): Decoder[LocalDate] =
    new StandardJavaTimeDecoder[LocalDate]("LocalDate") {
      protected final def parseUnsafe(input: String): LocalDate =
        LocalDate.parse(input, formatter)
    }

  /**
   * @group Time
   */
  final def decodeLocalTimeWithFormatter(formatter: DateTimeFormatter): Decoder[LocalTime] =
    new StandardJavaTimeDecoder[LocalTime]("LocalTime") {
      protected final def parseUnsafe(input: String): LocalTime =
        LocalTime.parse(input, formatter)
    }

  /**
   * @group Time
   */
  final def decodeLocalDateTimeWithFormatter(formatter: DateTimeFormatter): Decoder[LocalDateTime] =
    new StandardJavaTimeDecoder[LocalDateTime]("LocalDateTime") {
      protected final def parseUnsafe(input: String): LocalDateTime =
        LocalDateTime.parse(input, formatter)
    }

  /**
   * @group Time
   */
  final def decodeOffsetTimeWithFormatter(formatter: DateTimeFormatter): Decoder[OffsetTime] =
    new StandardJavaTimeDecoder[OffsetTime]("OffsetTime") {
      protected final def parseUnsafe(input: String): OffsetTime =
        OffsetTime.parse(input, formatter)
    }

  /**
   * @group Time
   */
  final def decodeOffsetDateTimeWithFormatter(formatter: DateTimeFormatter): Decoder[OffsetDateTime] =
    new StandardJavaTimeDecoder[OffsetDateTime]("OffsetDateTime") {
      protected final def parseUnsafe(input: String): OffsetDateTime =
        OffsetDateTime.parse(input, formatter)
    }

  /**
   * @group Time
   */
  final def decodeZonedDateTimeWithFormatter(formatter: DateTimeFormatter): Decoder[ZonedDateTime] =
    new StandardJavaTimeDecoder[ZonedDateTime]("ZonedDateTime") {
      protected final def parseUnsafe(input: String): ZonedDateTime =
        ZonedDateTime.parse(input, formatter)
    }

  /**
   * @group Time
   */
  final def decodeYearMonthWithFormatter(formatter: DateTimeFormatter): Decoder[YearMonth] =
    new StandardJavaTimeDecoder[YearMonth]("YearMonth") {
      protected final def parseUnsafe(input: String): YearMonth =
        YearMonth.parse(input, formatter)
    }

  /**
   * @group Time
   */
  implicit final val decodeLocalDate: Decoder[LocalDate] =
    new StandardJavaTimeDecoder[LocalDate]("LocalDate") {
      protected final def parseUnsafe(input: String): LocalDate =
        LocalDate.parse(input, ISO_LOCAL_DATE)
    }

  /**
   * @group Time
   */
  implicit final val decodeLocalTime: Decoder[LocalTime] =
    new StandardJavaTimeDecoder[LocalTime]("LocalTime") {
      protected final def parseUnsafe(input: String): LocalTime =
        LocalTime.parse(input, ISO_LOCAL_TIME)
    }

  /**
   * @group Time
   */
  implicit final val decodeLocalDateTime: Decoder[LocalDateTime] =
    new StandardJavaTimeDecoder[LocalDateTime]("LocalDateTime") {
      protected final def parseUnsafe(input: String): LocalDateTime =
        LocalDateTime.parse(input, ISO_LOCAL_DATE_TIME)
    }

  /**
   * @group Time
   */
  implicit final val decodeOffsetTime: Decoder[OffsetTime] =
    new StandardJavaTimeDecoder[OffsetTime]("OffsetTime") {
      protected final def parseUnsafe(input: String): OffsetTime =
        OffsetTime.parse(input, ISO_OFFSET_TIME)
    }

  /**
   * @group Time
   */
  implicit final val decodeOffsetDateTime: Decoder[OffsetDateTime] =
    new StandardJavaTimeDecoder[OffsetDateTime]("OffsetDateTime") {
      protected final def parseUnsafe(input: String): OffsetDateTime =
        OffsetDateTime.parse(input, ISO_OFFSET_DATE_TIME)
    }

  /**
   * @group Time
   */
  implicit final val decodeZonedDateTime: Decoder[ZonedDateTime] =
    new StandardJavaTimeDecoder[ZonedDateTime]("ZonedDateTime") {
      protected final def parseUnsafe(input: String): ZonedDateTime =
        ZonedDateTime.parse(input, ISO_ZONED_DATE_TIME)
    }

  /**
   * @group Time
   */
  implicit final val decodeYearMonth: Decoder[YearMonth] =
    new StandardJavaTimeDecoder[YearMonth]("YearMonth") {
      protected final def parseUnsafe(input: String): YearMonth =
        YearMonth.parse(input, DateTimeFormatter.ofPattern("yyyy-MM"))
    }
}
