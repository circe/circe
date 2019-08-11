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
 *
 * This representation is the same as that used by `java.math.BigDecimal`, with two differences.
 * First, the scale is a `java.math.BigInteger`, not a [[scala.Int]], and the unscaled value will
 * never be an exact multiple of ten (in order to facilitate comparison).
 */
final class BiggerDecimal(
  private[this] val _unscaled: BigInteger,
  private[this] val _scale: BigInteger,
  private[this] val _isNegativeZero: Boolean
) extends Serializable {
  def isWhole: Boolean = _scale.signum < 1

  /**
   * The sign of this value.
   *
   * Returns -1 if it is less than 0, +1 if it is greater than 0, and 0 if it is
   * equal to 0. Note that this follows the behavior of [[scala.Double]] for
   * negative zero (returning 0).
   */
  def signum: Int = unscaled.signum

  @inline private[this] def scaleIsInt: Boolean =
    _scale.compareTo(BiggerDecimal.MaxInt) <= 0 && _scale.compareTo(BiggerDecimal.MinInt) >= 0

  /**
   * Convert to a `java.math.BigDecimal` if the `scale` is within the range of [[scala.Int]].
   */
  def toBigDecimal: Option[BigDecimal] =
    if (scaleIsInt) {
      Some(new BigDecimal(_unscaled, _scale.intValue))
    } else None

  /**
   * Convert to a `java.math.BigInteger` if this is a sufficiently small whole number.
   *
   */
  def toBigIntegerWithMaxDigits(maxDigits: BigInteger): Option[BigInteger] =
    if (!isWhole) None
    else {
      val digits = BigInteger.valueOf(_unscaled.abs.toString.length.toLong).subtract(_scale)

      if (digits.compareTo(maxDigits) > 0) None
      else
        Some(
          new BigDecimal(_unscaled, _scale.intValue).toBigInteger
        )
    }

  /**
   * Convert to a `java.math.BigInteger` if this is a sufficiently small whole number.
   *
   * The maximum number of digits is somewhat arbitrarily set at 2^18 digits, since larger values
   * may require excessive processing power. Larger values may be converted to `BigInteger` with
   * [[toBigIntegerWithMaxDigits]] or via [[toBigDecimal]].
   */
  def toBigInteger: Option[BigInteger] = toBigIntegerWithMaxDigits(BiggerDecimal.MaxBigIntegerDigits)

  /**
   * Convert to the nearest [[scala.Double]].
   */
  def toDouble: Double =
    if (_isNegativeZero) {
      -0.0
    } else if (scale.compareTo(BiggerDecimal.MaxInt) <= 0 && scale.compareTo(BiggerDecimal.MinInt) >= 0) {
      new BigDecimal(unscaled, scale.intValue).doubleValue
    } else (if (scale.signum == 1) 0.0 else Double.PositiveInfinity) * unscaled.signum

  /**
   * Convert to the nearest [[scala.Float]].
   */
  def toFloat: Float =
    if (_isNegativeZero) {
      -0.0f
    } else if (scale.compareTo(BiggerDecimal.MaxInt) <= 0 && scale.compareTo(BiggerDecimal.MinInt) >= 0) {
      new BigDecimal(unscaled, scale.intValue).floatValue
    } else (if (scale.signum == 1) 0.0f else Float.PositiveInfinity) * unscaled.signum

  /**
   * Convert to a [[scala.Long]] if this is a valid `Long` value.
   */
  def toLong: Option[Long] = if (!this.isWhole) None
  else {
    toBigInteger match {
      case Some(i) =>
        val asLong = i.longValue

        if (BigInteger.valueOf(asLong) == i) Some(asLong) else None
      case None => None
    }
  }

  protected def unscaled: BigInteger = _unscaled
  protected def scale: BigInteger = _scale
  protected def isNegativeZero: Boolean = _isNegativeZero

  override def equals(that: Any): Boolean = that match {
    case other: BiggerDecimal =>
      _unscaled == other.unscaled && _scale == other.scale && _isNegativeZero == other.isNegativeZero
    case _ => false
  }

  override def hashCode: Int = _scale.hashCode + _unscaled.hashCode

  override def toString: String = if (_isNegativeZero) "-0"
  else if (_scale == BigInteger.ZERO) _unscaled.toString
  else {
    s"${_unscaled}e${_scale.negate}"
  }

  private[circe] def appendToStringBuilder(builder: StringBuilder): Unit =
    if (_isNegativeZero) builder.append("-0")
    else {
      builder.append(_unscaled)

      if (_scale != BigInteger.ZERO) {
        builder.append('e')
        builder.append(_scale.negate)
      }
    }
}

object BiggerDecimal {
  private val MaxBigIntegerDigits: BigInteger = BigInteger.valueOf(1L << 18)

  private val MaxInt: BigInteger = BigInteger.valueOf(Int.MaxValue)
  private val MinInt: BigInteger = BigInteger.valueOf(Int.MinValue)
  private val MaxLong: BigDecimal = new BigDecimal(Long.MaxValue)
  private val MinLong: BigDecimal = new BigDecimal(Long.MinValue)

  private[this] val UnsignedZero: BiggerDecimal = new BiggerDecimal(BigInteger.ZERO, BigInteger.ZERO, false)
  private[this] val NegativeZero: BiggerDecimal = new BiggerDecimal(BigInteger.ZERO, BigInteger.ZERO, true)

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

      new BiggerDecimal(current, BigInteger.valueOf(depth), false)
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

  private[this] final val FAILED = 0
  private[this] final val START = 1
  private[this] final val AFTER_ZERO = 2
  private[this] final val AFTER_DOT = 3
  private[this] final val FRACTIONAL = 4
  private[this] final val AFTER_E = 5
  private[this] final val AFTER_EXP_SIGN = 6
  private[this] final val EXPONENT = 7
  private[this] final val INTEGRAL = 8

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

      var state =
        if (i >= len) FAILED
        else {
          if (input.charAt(i) != '0') START
          else {
            i = i + 1
            AFTER_ZERO
          }
        }

      while (i < len && state != FAILED) {
        val c = input.charAt(i)

        (state: @switch) match {
          case START =>
            if (c >= '1' && c <= '9') {
              state = INTEGRAL
            } else {
              state = FAILED
            }
          case AFTER_ZERO =>
            if (c == '.') {
              state = AFTER_DOT
            } else if (c == 'e' || c == 'E') {
              state = AFTER_E
            } else {
              state = FAILED
            }
          case INTEGRAL =>
            if (c == '0') {
              zeros = zeros + 1
              state = INTEGRAL
            } else if (c >= '1' && c <= '9') {
              zeros = 0
              state = INTEGRAL
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

          new BiggerDecimal(unscaled, scale, false)
        }
      }
    }
  }
}
