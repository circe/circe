package io.circe.numbers

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
sealed abstract class JsonNumber extends Serializable {
  def scale: BigInteger
  def unscaled: BigInteger
  def isNegativeZero: Boolean

  final def isWhole: Boolean = scale.signum < 1

  /**
   * The sign of this value.
   *
   * Returns -1 if it is less than 0, +1 if it is greater than 0, and 0 if it is
   * equal to 0. Note that this follows the behavior of [[scala.Double]] for
   * negative zero (returning 0).
   */
  final def signum: Int = unscaled.signum

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
  final def toBigInteger: Option[BigInteger] = toBigIntegerWithMaxDigits(JsonNumber.MaxBigIntegerDigits)

  /**
   * Convert to the nearest [[scala.Double]].
   */
  def toDouble: Double

  /**
   * Convert to a [[scala.Byte]] if this is a valid `Byte` value.
   */
  def toByte: Option[Byte]

  /**
   * Convert to a [[scala.Short]] if this is a valid `Short` value.
   */
  def toShort: Option[Short]

  /**
   * Convert to a [[scala.Int]] if this is a valid `Int` value.
   */
  def toInt: Option[Int]

  /**
   * Convert to a [[scala.Long]] if this is a valid `Long` value.
   */
  def toLong: Option[Long]

  /**
   * Convert to the nearest [[scala.Byte]].
   */
  def truncateToByte: Byte

  /**
   * Convert to the nearest [[scala.Short]].
   */
  def truncateToShort: Short

  /**
   * Convert to the nearest [[scala.Int]].
   */
  def truncateToInt: Int

  /**
   * Convert to the nearest [[scala.Long]].
   */
  def truncateToLong: Long
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
) extends JsonNumber {
  def isNegativeZero: Boolean = false

  def toBigDecimal: Option[BigDecimal] =
    if (JsonNumber.bigIntegerIsValidInt(scale)) Some(new BigDecimal(unscaled, scale.intValue)) else None

  def toBigIntegerWithMaxDigits(maxDigits: BigInteger): Option[BigInteger] =
    if (!isWhole) None else {
      val digits = BigInteger.valueOf(unscaled.abs.toString.length.toLong).subtract(scale)

      if (digits.compareTo(JsonNumber.MaxBigIntegerDigits) > 0) None else Some(
        new BigDecimal(unscaled, scale.intValue).toBigInteger
      )
    }

  def toDouble: Double =
    if (JsonNumber.bigIntegerIsValidInt(scale)) {
      new BigDecimal(unscaled, scale.intValue).doubleValue
    } else (if (scale.signum == 1) 0.0 else Double.PositiveInfinity) * unscaled.signum

  def toByte: Option[Byte] = if (!isWhole) None else toBigInteger match {
    case Some(value) => if (JsonNumber.bigIntegerIsValidByte(value)) Some(value.byteValue) else None
    case None => None
  }

  def toShort: Option[Short] = if (!isWhole) None else toBigInteger match {
    case Some(value) => if (JsonNumber.bigIntegerIsValidShort(value)) Some(value.shortValue) else None
    case None => None
  }

  def toInt: Option[Int] = if (!isWhole) None else toBigInteger match {
    case Some(value) => if (JsonNumber.bigIntegerIsValidInt(value)) Some(value.intValue) else None
    case None => None
  }

  def toLong: Option[Long] = if (!isWhole) None else toBigInteger match {
    case Some(value) => if (JsonNumber.bigIntegerIsValidLong(value)) Some(value.longValue) else None
    case None => None
  }

  def truncateToByte: Byte = toByte match {
    case Some(value) => value
    case None => toBigDecimal match {
      case Some(asBigDecimal) =>
        val rounded = asBigDecimal.setScale(0, BigDecimal.ROUND_DOWN)

        if (rounded.compareTo(JsonNumber.MaxByte) >= 0) {
          Byte.MaxValue
        } else if (rounded.compareTo(JsonNumber.MinByte) <= 0) {
          Byte.MinValue
        } else rounded.byteValue
      case None => if (scale.signum > 0) 0 else if (unscaled.signum > 0) Byte.MaxValue else Byte.MinValue
    }
  }

  def truncateToShort: Short = toShort match {
    case Some(value) => value
    case None => toBigDecimal match {
      case Some(asBigDecimal) =>
        val rounded = asBigDecimal.setScale(0, BigDecimal.ROUND_DOWN)

        if (rounded.compareTo(JsonNumber.MaxShort) >= 0) {
          Short.MaxValue
        } else if (rounded.compareTo(JsonNumber.MinShort) <= 0) {
          Short.MinValue
        } else rounded.shortValue
      case None => if (scale.signum > 0) 0 else if (unscaled.signum > 0) Short.MaxValue else Short.MinValue
    }
  }

  def truncateToInt: Int = toInt match {
    case Some(value) => value
    case None => toBigDecimal match {
      case Some(asBigDecimal) =>
        val rounded = asBigDecimal.setScale(0, BigDecimal.ROUND_DOWN)

        if (rounded.compareTo(JsonNumber.MaxInt) >= 0) {
          Int.MaxValue
        } else if (rounded.compareTo(JsonNumber.MinInt) <= 0) {
          Int.MinValue
        } else rounded.intValue
      case None => if (scale.signum > 0) 0 else if (unscaled.signum > 0) Int.MaxValue else Int.MinValue
    }
  }

  def truncateToLong: Long = toLong match {
    case Some(value) => value
    case None => toBigDecimal match {
      case Some(asBigDecimal) =>
        val rounded = asBigDecimal.setScale(0, BigDecimal.ROUND_DOWN)

        if (rounded.compareTo(JsonNumber.MaxLong) >= 0) {
          Long.MaxValue
        } else if (rounded.compareTo(JsonNumber.MinLong) <= 0) {
          Long.MinValue
        } else rounded.longValue
      case None => if (scale.signum > 0) 0L else if (unscaled.signum > 0) Long.MaxValue else Long.MinValue
    }
  }

  override def equals(that: Any): Boolean = if (that.isInstanceOf[JsonNumber]) {
    val other: JsonNumber = that.asInstanceOf[JsonNumber]

    !other.isNegativeZero && unscaled == other.unscaled && scale == other.scale
  } else false

  override def hashCode: Int = scale.hashCode + unscaled.hashCode

  override def toString: String = if (scale == BigInteger.ZERO) unscaled.toString else {
    s"${ unscaled }e${ scale.negate }"
  }
}

private[numbers] final class LazyJsonNumber(value: String) extends JsonNumber {
  private[this] lazy val parsedValue: JsonNumber = JsonNumber.parseJsonNumberUnsafe(value)

  def scale: BigInteger = parsedValue.scale
  def unscaled: BigInteger = parsedValue.unscaled
  def isNegativeZero: Boolean = parsedValue.isNegativeZero

  def toBigDecimal: Option[BigDecimal] = parsedValue.toBigDecimal
  def toBigIntegerWithMaxDigits(maxDigits: BigInteger): Option[BigInteger] =
    parsedValue.toBigIntegerWithMaxDigits(maxDigits)
  def toDouble: Double = parsedValue.toDouble
  def toByte: Option[Byte] = parsedValue.toByte
  def toShort: Option[Short] = parsedValue.toShort
  def toInt: Option[Int] = parsedValue.toInt
  def toLong: Option[Long] = parsedValue.toLong
  def truncateToByte: Byte = parsedValue.truncateToByte
  def truncateToShort: Short = parsedValue.truncateToShort
  def truncateToInt: Int = parsedValue.truncateToInt
  def truncateToLong: Long = parsedValue.truncateToLong

  override def equals(that: Any): Boolean = if (that.isInstanceOf[JsonNumber]) {
    val other: JsonNumber = that.asInstanceOf[JsonNumber]

    unscaled == other.unscaled && scale == other.scale
  } else false

  override def hashCode: Int = parsedValue.hashCode
  override def toString: String = value
}

final object JsonNumber {
  private[numbers] val MaxBigIntegerDigits: BigInteger = BigInteger.valueOf(1L << 18)

  private[numbers] val MaxByte: BigDecimal = new BigDecimal(Byte.MaxValue)
  private[numbers] val MinByte: BigDecimal = new BigDecimal(Byte.MinValue)
  private[numbers] val MaxShort: BigDecimal = new BigDecimal(Short.MaxValue)
  private[numbers] val MinShort: BigDecimal = new BigDecimal(Short.MinValue)
  private[numbers] val MaxInt: BigDecimal = new BigDecimal(Int.MaxValue)
  private[numbers] val MinInt: BigDecimal = new BigDecimal(Int.MinValue)
  private[numbers] val MaxLong: BigDecimal = new BigDecimal(Long.MaxValue)
  private[numbers] val MinLong: BigDecimal = new BigDecimal(Long.MinValue)

  private[this] val MaxByteBigInteger: BigInteger = BigInteger.valueOf(Byte.MaxValue)
  private[this] val MinByteBigInteger: BigInteger = BigInteger.valueOf(Byte.MinValue)
  private[this] val MaxShortBigInteger: BigInteger = BigInteger.valueOf(Short.MaxValue)
  private[this] val MinShortBigInteger: BigInteger = BigInteger.valueOf(Short.MinValue)
  private[this] val MaxIntBigInteger: BigInteger = BigInteger.valueOf(Int.MaxValue)
  private[this] val MinIntBigInteger: BigInteger = BigInteger.valueOf(Int.MinValue)
  private[this] val MaxLongBigInteger: BigInteger = BigInteger.valueOf(Long.MaxValue)
  private[this] val MinLongBigInteger: BigInteger = BigInteger.valueOf(Long.MinValue)

  private[this] abstract class Zero extends JsonNumber {
    final def scale: BigInteger = BigInteger.ZERO
    final def unscaled: BigInteger = BigInteger.ZERO
    final val toBigDecimal: Option[BigDecimal] = Some(BigDecimal.ZERO)
    final def toBigIntegerWithMaxDigits(maxDigits: BigInteger): Option[BigInteger] = Some(BigInteger.ZERO)
    final val toByte: Option[Byte] = Some(truncateToByte)
    final val toShort: Option[Short] = Some(truncateToShort)
    final val toInt: Option[Int] = Some(truncateToInt)
    final val toLong: Option[Long] = Some(truncateToLong)
    final def truncateToByte: Byte = 0
    final def truncateToShort: Short = 0
    final def truncateToInt: Int = 0
    final def truncateToLong: Long = 0L
  }

  private[this] val UnsignedZero: JsonNumber = new Zero {
    final def isNegativeZero: Boolean = false
    final def toDouble: Double = 0.0

    final override def equals(that: Any): Boolean = if (that.isInstanceOf[JsonNumber]) {
      val other: JsonNumber = that.asInstanceOf[JsonNumber]

      !other.isNegativeZero && unscaled == other.unscaled && scale == other.scale
    } else false

    final override def hashCode: Int = (0.0).hashCode
    final override def toString: String = "0"
  }

  val NegativeZero: JsonNumber = new Zero {
    final def isNegativeZero: Boolean = true
    final def toDouble: Double = -0.0

    final override def equals(that: Any): Boolean = if (that.isInstanceOf[JsonNumber]) {
      that.asInstanceOf[JsonNumber].isNegativeZero
    } else false

    final override def hashCode: Int = (-0.0).hashCode
    final override def toString: String = "-0"
  }

  private[this] def fromUnscaledAndScale(unscaled: BigInteger, scale: Long): JsonNumber =
    if (unscaled == BigInteger.ZERO) UnsignedZero else {
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

  def fromBigInteger(i: BigInteger): JsonNumber = fromUnscaledAndScale(i, 0L)
  def fromBigDecimal(d: BigDecimal): JsonNumber = fromUnscaledAndScale(d.unscaledValue, d.scale.toLong)
  def fromLong(d: Long): JsonNumber = fromUnscaledAndScale(BigInteger.valueOf(d), 0L)

  def fromDouble(d: Double): JsonNumber = if (java.lang.Double.compare(d, -0.0) == 0) {
    NegativeZero
  } else fromBigDecimal(BigDecimal.valueOf(d))

  def fromFloat(f: Float): JsonNumber = if (java.lang.Float.compare(f, -0.0f) == 0) {
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

  def bigDecimalIsWhole(value: BigDecimal): Boolean =
    value.signum == 0 || value.scale <= 0 || value.stripTrailingZeros.scale <= 0

  def bigDecimalIsValidByte(value: BigDecimal): Boolean =
    bigDecimalIsWhole(value) && value.compareTo(MinByte) >= 0 && value.compareTo(MaxByte) <= 0

  def bigDecimalIsValidShort(value: BigDecimal): Boolean =
    bigDecimalIsWhole(value) && value.compareTo(MinShort) >= 0 && value.compareTo(MaxShort) <= 0

  def bigDecimalIsValidInt(value: BigDecimal): Boolean =
    bigDecimalIsWhole(value) && value.compareTo(MinInt) >= 0 && value.compareTo(MaxInt) <= 0

  def bigDecimalIsValidLong(value: BigDecimal): Boolean =
    bigDecimalIsWhole(value) && value.compareTo(MinLong) >= 0 && value.compareTo(MaxLong) <= 0

  def bigIntegerIsValidByte(value: BigInteger): Boolean =
    value.compareTo(MinByteBigInteger) >= 0 && value.compareTo(MaxByteBigInteger) <= 0

  def bigIntegerIsValidShort(value: BigInteger): Boolean =
    value.compareTo(MinShortBigInteger) >= 0 && value.compareTo(MaxShortBigInteger) <= 0

  def bigIntegerIsValidInt(value: BigInteger): Boolean =
    value.compareTo(MinIntBigInteger) >= 0 && value.compareTo(MaxIntBigInteger) <= 0

  def bigIntegerIsValidLong(value: BigInteger): Boolean =
    value.compareTo(MinLongBigInteger) >= 0 && value.compareTo(MaxLongBigInteger) <= 0

  /**
   * Lazily parse string into [[JsonNumber]].
   */
  def lazyJsonNumberUnsafe(input: String): JsonNumber = new LazyJsonNumber(input)

  /**
   * Parse string into [[JsonNumber]].
   */
  def parseJsonNumber(input: String): Option[JsonNumber] = Option(parseJsonNumberUnsafe(input))

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
   * Parse string into [[JsonNumber]], returning `null` on parsing failure.
   */
  def parseJsonNumberUnsafe(input: String): JsonNumber = {
    val len = input.length

    if (len == 0) null else {
      var zeros = 0
      var decIndex = -1
      var expIndex = -1
      var i = if (input.charAt(0) == '-') 1 else 0
      var c = input.charAt(i)

      var state = if (input.charAt(i) != '0') START else {
        i = i + 1
        AFTER_ZERO
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

      if (state == FAILED || state == AFTER_DOT || state == AFTER_E || state == AFTER_EXP_SIGN) null else {
        val integral = if (decIndex >= 0) input.substring(0, decIndex) else {
          if (expIndex == -1) input else {
            input.substring(0, expIndex)
          }
        }

        val fractional = if (decIndex == -1) "" else {
          if (expIndex == -1) input.substring(decIndex + 1) else {
            input.substring(decIndex + 1, expIndex)
          }
        }

        val unscaledString = integral + fractional
        val unscaled = new BigInteger(unscaledString.substring(0, unscaledString.length - zeros))

        if (unscaled == BigInteger.ZERO) {
          if (input.charAt(0) == '-') NegativeZero else UnsignedZero
        } else {
          val rescale = BigInteger.valueOf((fractional.length - zeros).toLong)
          val scale = if (expIndex == -1) rescale else {
            rescale.subtract(new BigInteger(input.substring(expIndex + 1)))
          }

          new SigAndExp(unscaled, scale)
        }
      }
    }
  }
}
