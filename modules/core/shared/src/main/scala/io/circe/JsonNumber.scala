package io.circe

import cats.Eq
import io.circe.numbers.{ BiggerDecimal, NumberParsing }

/**
 * A JSON number with optimization by cases.
 */
sealed abstract class JsonNumber extends Serializable {
  private[circe] def toBiggerDecimal: BiggerDecimal

  /**
   * Return this number as a [[scala.math.BigDecimal]].
   */
  def toBigDecimal: Option[BigDecimal]

  /**
   * Return this number as a [[scala.math.BigInt]] if it's a sufficiently small whole number.
   */
  def toBigInt: Option[BigInt]

  /**
   * Convert this number to its best [[scala.Double]] approximation.
   *
   * Anything over `Double.MaxValue` will be rounded to `Double.PositiveInfinity` and anything below
   * `Double.MinValue` is rounded to `Double.NegativeInfinity`.
   */
  def toDouble: Double

  /**
   * Return this number as a [[scala.Byte]] if it's a valid [[scala.Byte]].
   */
  final def toByte: Option[Byte] = toLong.flatMap { n =>
    val asByte: Byte = n.toByte
    if (n == asByte) Some(asByte) else None
  }

  /**
   * Return this number as a [[scala.Short]] if it's a valid [[scala.Short]].
   */
  final def toShort: Option[Short] = toLong.flatMap { n =>
    val asShort: Short = n.toShort
    if (n == asShort) Some(asShort) else None
  }

  /**
   * Return this number as an [[scala.Int]] if it's a valid [[scala.Int]].
   */
  final def toInt: Option[Int] = toLong.flatMap { n =>
    val asInt: Int = n.toInt
    if (n == asInt) Some(asInt) else None
  }

  /**
   * Return this number as a [[scala.Long]] if it's a valid [[scala.Long]].
   */
  def toLong: Option[Long]

  /**
   * Truncate the number to a [[scala.Byte]].
   *
   * Truncation means that we round toward zero to the closest valid [[scala.Byte]]. If the number
   * is `1e99`, for example, this will return `Byte.MaxValue`.
   */
  final def truncateToByte: Byte = {
    val asLong: Long = truncateToLong
    if (asLong > Byte.MaxValue) {
      Byte.MaxValue
    } else if (asLong < Byte.MinValue) {
      Byte.MinValue
    } else asLong.toByte
  }

  /**
   * Truncate the number to a [[scala.Short]].
   *
   * Truncation means that we round toward zero to the closest valid [[scala.Short]]. If the number
   * is `1e99`, for example, this will return `Short.MaxValue`.
   */
  final def truncateToShort: Short = {
    val asLong: Long = truncateToLong
    if (asLong > Short.MaxValue) {
      Short.MaxValue
    } else if (asLong < Short.MinValue) {
      Short.MinValue
    } else asLong.toShort
  }

  /**
   * Truncate the number to an [[scala.Int]].
   *
   * Truncation means that we round toward zero to the closest valid [[scala.Int]]. If the number is
   * `1e99`, for example, this will return `Int.MaxValue`.
   */
  final def truncateToInt: Int = {
    val asLong: Long = truncateToLong
    if (asLong > Int.MaxValue) {
      Int.MaxValue
    } else if (asLong < Int.MinValue) {
      Int.MinValue
    } else asLong.toInt
  }

  /**
   * Truncate the number to a [[scala.Long]].
   *
   * Truncation means that we round toward zero to the closest valid [[scala.Long]]. If the number
   * is `1e99`, for example, this will return `Long.MaxValue`.
   */
  def truncateToLong: Long

  /**
   * Universal equality derived from our type-safe equality.
   */
  override final def equals(that: Any): Boolean = that match {
    case that: JsonNumber => JsonNumber.eqJsonNumber.eqv(this, that)
    case _ => false
  }

  /**
   * Hashing that is consistent with our universal equality.
   */
  override final def hashCode: Int = toBiggerDecimal.hashCode
}

private[circe] sealed abstract class BiggerDecimalJsonNumber extends JsonNumber {
  final def toBigDecimal: Option[BigDecimal] = toBiggerDecimal.toBigDecimal.map(BigDecimal(_))
  final def toBigInt: Option[BigInt] = toBiggerDecimal.toBigInteger.map(BigInt(_))
  final def toDouble: Double = toBiggerDecimal.toDouble
  final def toLong: Option[Long] = toBiggerDecimal.toLong
  final def truncateToLong: Long = toBiggerDecimal.truncateToLong
}

/**
 * Represent a valid JSON number as a `String`.
 */
private[circe] final case class JsonDecimal(value: String) extends BiggerDecimalJsonNumber {
  private[circe] lazy val toBiggerDecimal: BiggerDecimal =
    NumberParsing.parseBiggerDecimal(value).getOrElse(
      throw new NumberFormatException("For input string \"" + value + "\"")
    )

  override def toString: String = value
}

private[circe] final case class JsonBiggerDecimal(value: BiggerDecimal)
  extends BiggerDecimalJsonNumber {
  private[circe] def toBiggerDecimal: BiggerDecimal = value
  override def toString: String = value.toString
}

/**
 * Represent a valid JSON number as a [[scala.math.BigDecimal]].
 */
private[circe] final case class JsonBigDecimal(value: BigDecimal) extends JsonNumber {
  private[circe] def toBiggerDecimal: BiggerDecimal = BiggerDecimal.fromBigDecimal(value.underlying)
  final def toBigDecimal: Option[BigDecimal] = Some(value)
  final def toBigInt: Option[BigInt] = toBiggerDecimal.toBigInteger.map(BigInt(_))
  final def toDouble: Double = value.toDouble
  final def toLong: Option[Long] = toBiggerDecimal.toLong
  final def truncateToLong: Long = math.round(toDouble)
  override final def toString: String = value.toString
}

/**
 * Represent a valid JSON number as a [[scala.Long]].
 */
private[circe] final case class JsonLong(value: Long) extends JsonNumber {
  private[circe] def toBiggerDecimal: BiggerDecimal = BiggerDecimal.fromLong(value)
  final def toBigDecimal: Option[BigDecimal] = Some(BigDecimal(value))
  final def toBigInt: Option[BigInt] = Some(BigInt(value))
  final def toDouble: Double = value.toDouble
  final def toLong: Option[Long] = Some(value)
  final def truncateToLong: Long = value
  override final def toString: String = java.lang.Long.toString(value)
}

/**
 * Represent a valid JSON number as a [[scala.Double]].
 */
private[circe] final case class JsonDouble(value: Double) extends JsonNumber {
  private[circe] def toBiggerDecimal: BiggerDecimal = BiggerDecimal.fromDouble(value)
  final def toBigDecimal: Option[BigDecimal] = Some(BigDecimal(value))
  final def toBigInt: Option[BigInt] = toBigDecimal.flatMap { d =>
    if (d.isWhole) Some(d.toBigInt) else None
  }
  final def toDouble: Double = value

  final def toLong: Option[Long] = {
    val asLong: Long = value.toLong
    if (asLong.toDouble == value) Some(asLong) else None
  }

  final def truncateToLong: Long = math.round(value)
  override final def toString: String = java.lang.Double.toString(value)
}

/**
 * Constructors, type class instances, and other utilities for [[JsonNumber]].
 */
final object JsonNumber {
  /**
   * Return a `JsonNumber` whose value is the valid JSON number in `value`.
   *
   * @note This value is ''not'' verified to be a valid JSON string. It is assumed that `value` is a
   * valid JSON number, according to the JSON specification. If the value is invalid the behavior is
   * undefined. This operation is provided for use in situations where the validity of the input has
   * already been verified.
   */
  final def unsafeDecimal(value: String): JsonNumber = JsonDecimal(value)

  /**
   * Return a `JsonNumber` whose value is the valid integral JSON number in `value`.
   *
   * @note This value is ''not'' verified to be a valid JSON string. It is assumed that `value` is a
   * valid JSON number, according to the JSON specification. If the value is invalid the behavior is
   * undefined. This operation is provided for use in situations where the validity of the input has
   * already been verified.
   */
  final def unsafeIntegral(value: String): JsonNumber =
    if (!NumberParsing.integralIsValidLong(value)) JsonDecimal(value) else {
      val longValue = java.lang.Long.parseLong(value)

      if (value.charAt(0) == '-' && longValue == 0L) JsonDecimal(value) else JsonLong(longValue)
    }

  final def fromString(value: String): Option[JsonNumber] =
    NumberParsing.parseBiggerDecimal(value).map(JsonBiggerDecimal(_))

  implicit final val eqJsonNumber: Eq[JsonNumber] = Eq.instance {
    case (JsonBiggerDecimal(a), b) => a == b.toBiggerDecimal
    case (a, JsonBiggerDecimal(b)) => a.toBiggerDecimal == b
    case (a @ JsonDecimal(_), b) => a.toBiggerDecimal == b.toBiggerDecimal
    case (a, b @ JsonDecimal(_)) => a.toBiggerDecimal == b.toBiggerDecimal
    case (JsonLong(x), JsonLong(y)) => x == y
    case (JsonDouble(x), JsonLong(y)) => x == y
    case (JsonLong(x), JsonDouble(y)) => y == x
    case (JsonDouble(x), JsonDouble(y)) => java.lang.Double.compare(x, y) == 0
    case (a, b) => a.toBigDecimal == b.toBigDecimal
  }
}
