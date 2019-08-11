package io.circe

import cats.kernel.Eq
import io.circe.numbers.BiggerDecimal
import java.io.Serializable
import java.lang.StringBuilder
import java.math.{ BigDecimal => JavaBigDecimal, BigInteger => JavaBigInteger }

/**
 * A JSON number with optimization by cases.
 */
sealed abstract class JsonNumber extends Serializable {
  protected def toBiggerDecimal: BiggerDecimal

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
   * Convert this number to its best [[scala.Float]] approximation.
   *
   * Anything over `Float.MaxValue` will be rounded to `Float.PositiveInfinity` and anything below
   * `Float.MinValue` is rounded to `Float.NegativeInfinity`.
   */
  def toFloat: Float

  /**
   * Return this number as a [[scala.Byte]] if it's a valid [[scala.Byte]].
   */
  final def toByte: Option[Byte] = toLong match {
    case Some(n) =>
      val asByte: Byte = n.toByte
      if (n == asByte) Some(asByte) else None
    case None => None
  }

  /**
   * Return this number as a [[scala.Short]] if it's a valid [[scala.Short]].
   */
  final def toShort: Option[Short] = toLong match {
    case Some(n) =>
      val asShort: Short = n.toShort
      if (n == asShort) Some(asShort) else None
    case None => None
  }

  /**
   * Return this number as an [[scala.Int]] if it's a valid [[scala.Int]].
   */
  final def toInt: Option[Int] = toLong match {
    case Some(n) =>
      val asInt: Int = n.toInt
      if (n == asInt) Some(asInt) else None
    case None => None
  }

  /**
   * Return this number as a [[scala.Long]] if it's a valid [[scala.Long]].
   */
  def toLong: Option[Long]

  /**
   * Hashing that is consistent with our universal equality.
   */
  override final def hashCode: Int = toBiggerDecimal.hashCode

  private[circe] def appendToStringBuilder(builder: StringBuilder): Unit
}

/**
 * Constructors, type class instances, and other utilities for [[JsonNumber]].
 */
object JsonNumber {
  private[this] sealed abstract class BiggerDecimalJsonNumber extends JsonNumber {
    final def toBigDecimal: Option[BigDecimal] = toBiggerDecimal.toBigDecimal.map(BigDecimal(_))
    final def toBigInt: Option[BigInt] = toBiggerDecimal.toBigInteger.map(BigInt(_))
    final def toLong: Option[Long] = toBiggerDecimal.toLong

    override final def equals(that: Any): Boolean = that match {
      case other: JsonNumber => toBiggerDecimal == other.toBiggerDecimal
      case _                 => false
    }
  }

  /**
   * Represent a valid JSON number as a `String`.
   */
  private[this] final case class JsonDecimal(value: String) extends BiggerDecimalJsonNumber {
    protected lazy val toBiggerDecimal: BiggerDecimal = {
      val result = BiggerDecimal.parseBiggerDecimalUnsafe(value)

      if (result.eq(null)) {
        throw new NumberFormatException("For input string \"" + value + "\"")
      } else result
    }

    final def toDouble: Double = java.lang.Double.parseDouble(value)
    final def toFloat: Float = java.lang.Float.parseFloat(value)
    override def toString: String = value
    private[circe] def appendToStringBuilder(builder: StringBuilder): Unit = builder.append(value)
  }

  private[this] final case class JsonBiggerDecimal(value: BiggerDecimal) extends BiggerDecimalJsonNumber {
    protected def toBiggerDecimal: BiggerDecimal = value
    final def toDouble: Double = toBiggerDecimal.toDouble
    final def toFloat: Float = toBiggerDecimal.toFloat
    override def toString: String = value.toString
    private[circe] def appendToStringBuilder(builder: StringBuilder): Unit = value.appendToStringBuilder(builder)
  }

  /**
   * Represent a valid JSON number as a `java.math.BigDecimal`.
   */
  private[this] final case class JsonBigDecimal(value: JavaBigDecimal) extends JsonNumber {
    protected def toBiggerDecimal: BiggerDecimal = BiggerDecimal.fromBigDecimal(value)
    final def toBigDecimal: Option[BigDecimal] = Some(new BigDecimal(value))
    final def toBigInt: Option[BigInt] = toBiggerDecimal.toBigInteger.map(BigInt(_))
    final def toDouble: Double = value.doubleValue
    final def toFloat: Float = value.floatValue
    final def toLong: Option[Long] = toBiggerDecimal.toLong
    override final def toString: String = value.toString
    private[circe] def appendToStringBuilder(builder: StringBuilder): Unit = builder.append(value.toString)

    override final def equals(that: Any): Boolean = that match {
      case other: JsonBigDecimal => value.compareTo(other.value) == 0
      case other: JsonNumber     => toBiggerDecimal == other.toBiggerDecimal
      case _                     => false
    }
  }

  /**
   * Represent a valid JSON number as a [[scala.Long]].
   */
  private[this] final case class JsonLong(value: Long) extends JsonNumber {
    protected def toBiggerDecimal: BiggerDecimal = BiggerDecimal.fromLong(value)
    final def toBigDecimal: Option[BigDecimal] = Some(BigDecimal(value))
    final def toBigInt: Option[BigInt] = Some(BigInt(value))
    final def toDouble: Double = value.toDouble
    final def toFloat: Float = value.toFloat
    final def toLong: Option[Long] = Some(value)
    override final def toString: String = java.lang.Long.toString(value)
    private[circe] def appendToStringBuilder(builder: StringBuilder): Unit = builder.append(value)

    override final def equals(that: Any): Boolean = that match {
      case other: JsonLong   => value == other.value
      case other: JsonNumber => toBiggerDecimal == other.toBiggerDecimal
      case _                 => false
    }
  }

  /**
   * Represent a valid JSON number as a [[scala.Double]].
   */
  private[this] final case class JsonDouble(value: Double) extends JsonNumber {
    protected def toBiggerDecimal: BiggerDecimal = BiggerDecimal.fromDoubleUnsafe(value)
    private[this] def toJavaBigDecimal = JavaBigDecimal.valueOf(value)

    final def toBigDecimal: Option[BigDecimal] = Some(toJavaBigDecimal)
    final def toBigInt: Option[BigInt] = {
      val asBigDecimal = toJavaBigDecimal

      if (JsonNumber.bigDecimalIsWhole(asBigDecimal)) Some(new BigInt(asBigDecimal.toBigInteger)) else None
    }

    final def toDouble: Double = value
    final def toFloat: Float = value.toFloat

    final def toLong: Option[Long] = {
      val asBigDecimal = toJavaBigDecimal

      if (JsonNumber.bigDecimalIsValidLong(asBigDecimal)) Some(asBigDecimal.longValue) else None
    }

    override final def toString: String = java.lang.Double.toString(value)
    private[circe] def appendToStringBuilder(builder: StringBuilder): Unit = builder.append(value)

    override final def equals(that: Any): Boolean = that match {
      case other: JsonDouble => java.lang.Double.compare(value, other.value) == 0
      case other: JsonNumber => toBiggerDecimal == other.toBiggerDecimal
      case _                 => false
    }
  }

  /**
   * Represent a valid JSON number as a [[scala.Float]].
   */
  private[this] final case class JsonFloat(value: Float) extends JsonNumber {
    protected def toBiggerDecimal: BiggerDecimal = BiggerDecimal.fromFloat(value)
    private[this] def toJavaBigDecimal = new JavaBigDecimal(java.lang.Float.toString(value))

    final def toBigDecimal: Option[BigDecimal] = Some(toJavaBigDecimal)
    final def toBigInt: Option[BigInt] = {
      val asBigDecimal = toJavaBigDecimal

      if (JsonNumber.bigDecimalIsWhole(asBigDecimal)) Some(new BigInt(asBigDecimal.toBigInteger)) else None
    }

    // Don't use `value.toFloat` due to floating point errors.
    final def toDouble: Double = toJavaBigDecimal.doubleValue

    final def toFloat: Float = value

    final def toLong: Option[Long] = {
      val asBigDecimal = toJavaBigDecimal

      if (JsonNumber.bigDecimalIsValidLong(asBigDecimal)) Some(asBigDecimal.longValue) else None
    }

    override final def toString: String = java.lang.Float.toString(value)
    private[circe] def appendToStringBuilder(builder: StringBuilder): Unit = builder.append(value)

    override final def equals(that: Any): Boolean = that match {
      case other: JsonFloat  => java.lang.Float.compare(value, other.value) == 0
      case other: JsonNumber => toBiggerDecimal == other.toBiggerDecimal
      case _                 => false
    }
  }

  /**
   * Return a `JsonNumber` whose value is the valid JSON number in `value`.
   *
   * @note This value is ''not'' verified to be a valid JSON string. It is assumed that `value` is a
   * valid JSON number, according to the JSON specification. If the value is invalid the behavior is
   * undefined. This operation is provided for use in situations where the validity of the input has
   * already been verified.
   */
  final def fromDecimalStringUnsafe(value: String): JsonNumber = JsonDecimal(value)

  private[this] final val MaxLongString = "9223372036854775807"
  private[this] final val MinLongString = "-9223372036854775808"

  /**
   * Is a string representing an integral value a valid [[scala.Long]]?
   *
   * Note that this method assumes that the input is a valid integral JSON
   * number string (e.g. that it does have leading zeros).
   */
  private[this] def integralIsValidLong(s: String): Boolean = {
    val bound = if (s.charAt(0) == '-') MinLongString else MaxLongString

    s.length < bound.length || (s.length == bound.length && s.compareTo(bound) <= 0)
  }

  /**
   * Return a `JsonNumber` whose value is the valid integral JSON number in `value`.
   *
   * @note This value is ''not'' verified to be a valid JSON string. It is assumed that `value` is a
   * valid JSON number, according to the JSON specification. If the value is invalid the behavior is
   * undefined. This operation is provided for use in situations where the validity of the input has
   * already been verified.
   */
  final def fromIntegralStringUnsafe(value: String): JsonNumber =
    if (!integralIsValidLong(value)) JsonDecimal(value)
    else {
      val longValue = java.lang.Long.parseLong(value)

      if (value.charAt(0) == '-' && longValue == 0L) JsonDecimal(value) else JsonLong(longValue)
    }

  final def fromString(value: String): Option[JsonNumber] = {
    val result = BiggerDecimal.parseBiggerDecimalUnsafe(value)

    if (result.eq(null)) None else Some(JsonBiggerDecimal(result))
  }

  private[this] val bigDecimalMinLong: JavaBigDecimal = new JavaBigDecimal(Long.MinValue)
  private[this] val bigDecimalMaxLong: JavaBigDecimal = new JavaBigDecimal(Long.MaxValue)

  private[circe] def bigDecimalIsWhole(value: JavaBigDecimal): Boolean =
    value.signum == 0 || value.scale <= 0 || value.stripTrailingZeros.scale <= 0

  private[circe] def bigDecimalIsValidLong(value: JavaBigDecimal): Boolean =
    bigDecimalIsWhole(value) && value.compareTo(bigDecimalMinLong) >= 0 && value.compareTo(bigDecimalMaxLong) <= 0

  implicit final val eqJsonNumber: Eq[JsonNumber] = Eq.fromUniversalEquals

  private[circe] def fromLong(value: Long): JsonNumber = JsonLong(value)
  private[circe] def fromDoubleUnsafe(value: Double): JsonNumber = JsonDouble(value)
  private[circe] def fromFloatUnsafe(value: Float): JsonNumber = JsonFloat(value)
  private[circe] def fromBigDecimal(value: JavaBigDecimal): JsonNumber = JsonBigDecimal(value)
  private[circe] def fromBigInteger(value: JavaBigInteger): JsonNumber = JsonBiggerDecimal(
    BiggerDecimal.fromBigInteger(value)
  )
  private[circe] def fromBiggerDecimal(value: BiggerDecimal): JsonNumber = JsonBiggerDecimal(value)
}
