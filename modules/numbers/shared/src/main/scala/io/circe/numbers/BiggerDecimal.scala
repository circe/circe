package io.circe.numbers

import java.io.Serializable
import java.lang.StringBuilder
import java.math.{ BigDecimal, BigInteger }

/**
 * Represents a large decimal number.
 *
 * In theory `BigDecimal` can represent a very large range of valid JSON numbers (in most cases if a
 * JSON number string can fit in memory, it's possible to construct an exact `BigDecimal`
 * representation), but in practice this becomes intractable for many small JSON numbers (e.g.
 * "1e2147483648" cannot be directly parsed as a `BigDecimal`).
 *
 * This type makes it possible to represent much, much larger numbers efficiently (although it
 * doesn't support many operations on these values). It also makes it possible to distinguish
 * between positive and negative zeros (unlike `BigDecimal`), which may be useful in some
 * applications.
 */
sealed abstract class BiggerDecimal extends Serializable {
  def isWhole: Boolean
  def isNegativeZero: Boolean

  /**
   * The sign of this value.
   *
   * Returns -1 if it is less than 0, +1 if it is greater than 0, and 0 if it is
   * equal to 0. Note that this follows the behavior of [[scala.Double]] for
   * negative zero (returning 0).
   */
  def signum: Int

  /**
   * Convert to a `java.math.BigDecimal` if the `scale` is within the range of [[scala.Int]].
   */
  def toBigDecimal: Option[BigDecimal]

  /**
   * Convert to a `java.math.BigInteger` if this is a sufficiently small whole number.
   *
   */
  def toBigIntegerWithMaxDigits(maxDigits: BigInteger): Option[BigInteger]

  /**
   * Convert to a `java.math.BigInteger` if this is a sufficiently small whole number.
   *
   * The maximum number of digits is somewhat arbitrarily set at 2^18 digits, since larger values
   * may require excessive processing power. Larger values may be converted to `BigInteger` with
   * [[toBigIntegerWithMaxDigits]] or via [[toBigDecimal]].
   */
  final def toBigInteger: Option[BigInteger] = toBigIntegerWithMaxDigits(BiggerDecimal.MaxBigIntegerDigits)

  /**
   * Convert to the nearest [[scala.Double]].
   */
  def toDouble: Double

  /**
   * Convert to a [[scala.Long]] if this is a valid `Long` value.
   */
  def toLong: Option[Long]

  /**
   * Convert to the nearest [[scala.Long]].
   */
  def truncateToLong: Long

  private[circe] def appendToStringBuilder(builder: StringBuilder): Unit
}

/**
 * Represents numbers as an unscaled value and a scale.
 *
 * This representation is the same as that used by `java.math.BigDecimal`, with two differences.
 * First, the scale is a `java.math.BigInteger`, not a [[scala.Int]], and the unscaled value will
 * never be an exact multiple of ten (in order to facilitate comparison).
 */
private[numbers] final class SigAndExp(
  val unscaled: BigInteger,
  val scale: BigInteger
) extends BiggerDecimal {
  def isWhole: Boolean = scale.signum < 1
  def isNegativeZero: Boolean = false
  def signum: Int = unscaled.signum

  def toBigDecimal: Option[BigDecimal] =
    if (scale.compareTo(BiggerDecimal.MaxInt) <= 0 && scale.compareTo(BiggerDecimal.MinInt) >= 0) {
      Some(new BigDecimal(unscaled, scale.intValue))
    } else None

  def toBigIntegerWithMaxDigits(maxDigits: BigInteger): Option[BigInteger] =
    if (!isWhole) None else {
      val digits = BigInteger.valueOf(unscaled.abs.toString.length.toLong).subtract(scale)

      if (digits.compareTo(maxDigits) > 0) None else Some(
        new BigDecimal(unscaled, scale.intValue).toBigInteger
      )
    }

  def toDouble: Double = if (scale.compareTo(BiggerDecimal.MaxInt) <= 0 && scale.compareTo(BiggerDecimal.MinInt) >= 0) {
    new BigDecimal(unscaled, scale.intValue).doubleValue
  } else (if (scale.signum == 1) 0.0 else Double.PositiveInfinity) * unscaled.signum

  def toLong: Option[Long] = if (!this.isWhole) None else {
    toBigInteger match {
      case Some(i) =>
        val asLong = i.longValue

        if (BigInteger.valueOf(asLong) == i) Some(asLong) else None
      case None => None
    }
  }

  def truncateToLong: Long = toLong.getOrElse {
    toBigDecimal.map { asBigDecimal =>
      val rounded = asBigDecimal.setScale(0, BigDecimal.ROUND_DOWN)

      if (rounded.compareTo(BiggerDecimal.MaxLong) >= 0) {
        Long.MaxValue
      } else if (rounded.compareTo(BiggerDecimal.MinLong) <= 0) {
        Long.MinValue
      } else rounded.longValue
    }.getOrElse {
      if (scale.signum > 0) 0L else if (unscaled.signum > 0) Long.MaxValue else Long.MinValue
    }
  }

  override def equals(that: Any): Boolean = that match {
    case other: SigAndExp => unscaled == other.unscaled && scale == other.scale
    case _ => false
  }

  override def hashCode: Int = scale.hashCode + unscaled.hashCode

  override def toString: String = if (scale == BigInteger.ZERO) unscaled.toString else {
    s"${ unscaled }e${ scale.negate }"
  }

  private[circe] def appendToStringBuilder(builder: StringBuilder): Unit = {
    builder.append(unscaled)

    if (scale != BigInteger.ZERO) {
      builder.append('e')
      builder.append(scale.negate)
    }
  }
}

final object BiggerDecimal {
  private[numbers] val MaxBigIntegerDigits: BigInteger = BigInteger.valueOf(1L << 18)

  private[numbers] val MaxInt: BigInteger = BigInteger.valueOf(Int.MaxValue)
  private[numbers] val MinInt: BigInteger = BigInteger.valueOf(Int.MinValue)
  private[numbers] val MaxLong: BigDecimal = new BigDecimal(Long.MaxValue)
  private[numbers] val MinLong: BigDecimal = new BigDecimal(Long.MinValue)

  private[this] abstract class Zero extends BiggerDecimal {
    final def isWhole: Boolean = true
    final def signum: Int = 0
    final val toBigDecimal: Option[BigDecimal] = Some(BigDecimal.ZERO)
    final def toBigIntegerWithMaxDigits(maxDigits: BigInteger): Option[BigInteger] = Some(BigInteger.ZERO)
    final val toLong: Option[Long] = Some(truncateToLong)
    final def truncateToLong: Long = 0L

    private[circe] def appendToStringBuilder(builder: StringBuilder): Unit = {
      builder.append(toString)
    }
  }

  private[this] val UnsignedZero: BiggerDecimal = new Zero {
    final def isNegativeZero: Boolean = false
    final def toDouble: Double = 0.0

    final override def equals(that: Any): Boolean = that match {
      case other: Zero => !other.isNegativeZero
      case _ => false
    }
    final override def hashCode: Int = (0.0).hashCode
    final override def toString: String = "0"
  }

  val NegativeZero: BiggerDecimal = new Zero {
    final def isNegativeZero: Boolean = true
    final def toDouble: Double = -0.0

    final override def equals(that: Any): Boolean = that match {
      case other: Zero => other.isNegativeZero
      case _ => false
    }
    final override def hashCode: Int = (-0.0).hashCode
    final override def toString: String = "-0"
  }

  final def apply(unscaled: BigInteger, scale: BigInteger): BiggerDecimal = new SigAndExp(unscaled, scale)

  final def fromLong(value: Long): BiggerDecimal = parser.fromLong(value)
  final def fromBigInteger(value: BigInteger): BiggerDecimal = parser.fromBigInteger(value)
  final def fromBigDecimal(value: BigDecimal): BiggerDecimal = parser.fromBigDecimal(value)

  /**
   * Convert a [[scala.Double]] into a [[BiggerDecimal]].
   *
   * @note This method assumes that the input is not `NaN` or infinite, and will return `null` if
   *       that assumption does not hold.
   */
  def fromDoubleUnsafe(value: Double): BiggerDecimal = parser.fromDouble(value)

  /**
   * Convert a [[scala.Float]] into a [[BiggerDecimal]].
   *
   * @note This method assumes that the input is not `NaN` or infinite, and will return `null` if
   *       that assumption does not hold.
   */
  def fromFloatUnsafe(value: Float): BiggerDecimal = parser.fromFloat(value)

  private[this] final val parser: JsonNumberParser[BiggerDecimal] = new JsonNumberParser[BiggerDecimal] {
    def createNegativeZeroValue: BiggerDecimal = NegativeZero
    def createUnsignedZeroValue: BiggerDecimal = UnsignedZero
    def createLongValue(value: Long): BiggerDecimal = {
      var current = value
      var depth = 0

      while (current % 10 == 0) {
        current /= 10
        depth -= 1
      }

      apply(BigInteger.valueOf(current), BigInteger.valueOf(depth.toLong))
    }
    def createBigDecimalValue(unscaled: BigInteger, scale: Int): BiggerDecimal =
      apply(unscaled, BigInteger.valueOf(scale.toLong))
    def createBiggerDecimalValue(unscaled: BigInteger, scale: BigInteger): BiggerDecimal = apply(unscaled, scale)
    def failureValue: BiggerDecimal = null
  }

  /**
   * Parse string into [[BiggerDecimal]].
   */
  def parseBiggerDecimal(input: String): Option[BiggerDecimal] = Option(parseBiggerDecimalUnsafe(input))

  /**
   * Parse string into [[BiggerDecimal]], returning `null` on parsing failureValueure.
   */
  def parseBiggerDecimalUnsafe(input: String): BiggerDecimal = if (!parser.validate(input)) null else {
    parser.parseUnsafe(input)
  }
}
