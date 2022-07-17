package io.circe.numbers

import java.io.Serializable
import java.lang.StringBuilder
import java.math.{ BigDecimal, BigInteger }
import scala.annotation.switch

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
   * Convert to the nearest [[scala.Float]].
   */
  def toFloat: Float

  /**
   * Convert to a [[scala.Long]] if this is a valid `Long` value.
   */
  def toLong: Option[Long]

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
    if (!isWhole) None
    else {
      val digits = BigInteger.valueOf(unscaled.abs.toString.length.toLong).subtract(scale)

      if (digits.compareTo(maxDigits) > 0) None
      else
        Some(
          new BigDecimal(unscaled, scale.intValue).toBigInteger
        )
    }

  def toDouble: Double = if (scale.compareTo(BiggerDecimal.MaxInt) <= 0 && scale.compareTo(BiggerDecimal.MinInt) >= 0) {
    new BigDecimal(unscaled, scale.intValue).doubleValue
  } else (if (scale.signum == 1) 0.0 else Double.PositiveInfinity) * unscaled.signum

  def toFloat: Float = if (scale.compareTo(BiggerDecimal.MaxInt) <= 0 && scale.compareTo(BiggerDecimal.MinInt) >= 0) {
    new BigDecimal(unscaled, scale.intValue).floatValue
  } else (if (scale.signum == 1) 0.0f else Float.PositiveInfinity) * unscaled.signum

  def toLong: Option[Long] = if (!this.isWhole) None
  else {
    toBigInteger match {
      case Some(i) =>
        val asLong = i.longValue

        if (BigInteger.valueOf(asLong) == i) Some(asLong) else None
      case None => None
    }
  }

  override def equals(that: Any): Boolean = that match {
    case other: SigAndExp => unscaled == other.unscaled && scale == other.scale
    case _                => false
  }

  override def hashCode: Int = scale.hashCode + unscaled.hashCode

  override def toString: String = if (scale == BigInteger.ZERO) unscaled.toString
  else {
    s"${unscaled}e${scale.negate}"
  }

  private[circe] def appendToStringBuilder(builder: StringBuilder): Unit = {
    builder.append(unscaled)

    if (scale != BigInteger.ZERO) {
      builder.append('e')
      builder.append(scale.negate)
    }
  }
}

object BiggerDecimal {
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
    final val toLong: Option[Long] = Some(0L)

    private[circe] def appendToStringBuilder(builder: StringBuilder): Unit =
      builder.append(toString)
  }

  private[this] val UnsignedZero: BiggerDecimal = new Zero {
    final def isNegativeZero: Boolean = false
    final def toDouble: Double = 0.0
    final def toFloat: Float = 0.0f

    final override def equals(that: Any): Boolean = that match {
      case other: Zero => !other.isNegativeZero
      case _           => false
    }
    final override def hashCode: Int = 0.0.hashCode
    final override def toString: String = "0"
  }

  val NegativeZero: BiggerDecimal = new Zero {
    final def isNegativeZero: Boolean = true
    final def toDouble: Double = -0.0
    final def toFloat: Float = -0.0f

    final override def equals(that: Any): Boolean = that match {
      case other: Zero => other.isNegativeZero
      case _           => false
    }
    final override def hashCode: Int = -0.0.hashCode
    final override def toString: String = "-0"
  }

  private[this] def fromUnscaledAndScale(unscaled: BigInteger, scale: Long): BiggerDecimal =
    if (unscaled == BigInteger.ZERO) UnsignedZero
    else {
      var current = unscaled
      var depth = scale

      var divAndRem = current.divideAndRemainder(BigInteger.TEN)

      while (divAndRem(1) == BigInteger.ZERO) {
        current = divAndRem(0)
        depth -= 1L
        divAndRem = current.divideAndRemainder(BigInteger.TEN)
      }

      new SigAndExp(current, BigInteger.valueOf(depth))
    }

  def fromBigInteger(i: BigInteger): BiggerDecimal = fromUnscaledAndScale(i, 0L)
  def fromBigDecimal(d: BigDecimal): BiggerDecimal = fromUnscaledAndScale(d.unscaledValue, d.scale.toLong)
  def fromLong(d: Long): BiggerDecimal = fromUnscaledAndScale(BigInteger.valueOf(d), 0L)

  /**
   * Convert a [[scala.Double]] into a [[BiggerDecimal]].
   *
   * @note This method assumes that the input is not `NaN` or infinite, and will throw a
   *       `NumberFormatException` if that assumption does not hold.
   */
  def fromDoubleUnsafe(d: Double): BiggerDecimal = if (java.lang.Double.compare(d, -0.0) == 0) {
    NegativeZero
  } else fromBigDecimal(BigDecimal.valueOf(d))

  def fromFloat(f: Float): BiggerDecimal = if (java.lang.Float.compare(f, -0.0f) == 0) {
    NegativeZero
  } else fromBigDecimal(new BigDecimal(java.lang.Float.toString(f)))

  private[this] final val MaxLongString = "9223372036854775807"
  private[this] final val MinLongString = "-9223372036854775808"

  /**
   * Is a string representing an integral value a valid [[scala.Long]]?
   *
   * Note that this method assumes that the input is a valid integral JSON
   * number string (e.g. that it does have leading zeros).
   */
  def integralIsValidLong(s: String): Boolean = {
    val bound = if (s.charAt(0) == '-') MinLongString else MaxLongString

    s.length < bound.length || (s.length == bound.length && s.compareTo(bound) <= 0)
  }

  private[this] final val FAILED = 0
  private[this] final val AFTER_DOT = 1
  private[this] final val FRACTIONAL = 2
  private[this] final val AFTER_E = 3
  private[this] final val AFTER_EXP_SIGN = 4
  private[this] final val EXPONENT = 5
  private[this] final val INTEGRAL = 6

  /**
   * Parse string into [[BiggerDecimal]].
   */
  def parseBiggerDecimal(input: String): Option[BiggerDecimal] = Option(parseBiggerDecimalUnsafe(input))

  /**
   * Parse string into [[BiggerDecimal]], returning `null` on parsing failure.
   */
  def parseBiggerDecimalUnsafe(input: String): BiggerDecimal = {
    val len = input.length

    if (len == 0) null
    else {
      var zeros = 0
      var decIndex = -1
      var expIndex = -1
      var i = if (input.charAt(0) == '-') 1 else 0
      var parsedNonZeroIntegralDigit: Boolean = false

      if (i >= len) {
        null // state = FAILED
      } else {
        var state = INTEGRAL

        while (i < len && state != FAILED) {
          val c = input.charAt(i)
          (state: @switch) match {
            case INTEGRAL =>
              if (c == '0') {
                if (parsedNonZeroIntegralDigit) {
                  zeros = zeros + 1
                }
              } else if (c >= '1' && c <= '9') {
                parsedNonZeroIntegralDigit = true
                zeros = 0
              } else if (c == '.') {
                state = AFTER_DOT
              } else if (c == 'e' || c == 'E') {
                state = AFTER_E
              } else {
                state = FAILED
              }
            case AFTER_DOT =>
              decIndex = i - 1
              if (c == '0') {
                zeros = zeros + 1
                state = FRACTIONAL
              } else if (c >= '1' && c <= '9') {
                zeros = 0
                state = FRACTIONAL
              } else {
                state = FAILED
              }
            case AFTER_E =>
              expIndex = i - 1
              if (c >= '0' && c <= '9') {
                state = EXPONENT
              } else if (c == '+' || c == '-') {
                state = AFTER_EXP_SIGN
              } else {
                state = FAILED
              }
            case FRACTIONAL =>
              if (c == '0') {
                zeros = zeros + 1
                state = FRACTIONAL
              } else if (c >= '1' && c <= '9') {
                zeros = 0
                state = FRACTIONAL
              } else if (c == 'e' || c == 'E') {
                state = AFTER_E
              } else {
                state = FAILED
              }
            case AFTER_EXP_SIGN =>
              if (c >= '0' && c <= '9') {
                state = EXPONENT
              } else {
                state = FAILED
              }
            case EXPONENT =>
              if (c >= '0' && c <= '9') {
                state = EXPONENT
              } else {
                state = FAILED
              }
          }

          i += 1
        }

        if (state == FAILED || state == AFTER_DOT || state == AFTER_E || state == AFTER_EXP_SIGN) null
        else {
          val integral =
            if (decIndex >= 0) input.substring(0, decIndex)
            else {
              if (expIndex == -1) input
              else {
                input.substring(0, expIndex)
              }
            }

          val fractional =
            if (decIndex == -1) ""
            else {
              if (expIndex == -1) input.substring(decIndex + 1)
              else {
                input.substring(decIndex + 1, expIndex)
              }
            }

          val unscaledString = integral + fractional
          val unscaled = new BigInteger(unscaledString.substring(0, unscaledString.length - zeros))

          if (unscaled == BigInteger.ZERO) {
            if (input.charAt(0) == '-') NegativeZero else UnsignedZero
          } else {
            val rescale = BigInteger.valueOf((fractional.length - zeros).toLong)
            val scale =
              if (expIndex == -1) rescale
              else {
                rescale.subtract(new BigInteger(input.substring(expIndex + 1)))
              }

            new SigAndExp(unscaled, scale)
          }
        }
      }
    }
  }
}
