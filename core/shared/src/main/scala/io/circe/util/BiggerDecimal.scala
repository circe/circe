package io.circe.util

import java.math.{ BigDecimal, BigInteger }
import scala.annotation.tailrec

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
sealed abstract class BiggerDecimal {
  def isWhole: Boolean
  def isNegativeZero: Boolean

  /**
   * Convert to a `java.math.BigDecimal` if the `scale` is within the range of [[scala.Int]].
   */
  def toBigDecimal: Option[BigDecimal]

  /**
   * Convert to a `java.math.BigInteger` if this is a sufficiently small whole number.
   *
   * The size limit is somewhat arbitrarily set at 2^18 digits, since larger values may require
   * excessive processing power. Larger values may be converted to `BigInteger` via `toBigDecimal`.
   */
  def toBigInteger: Option[BigInteger]

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
}

/**
 * Represents numbers as an unscaled value and a scale.
 *
 * This representation is the same as that used by `java.math.BigDecimal`, with two differences.
 * First, the scale is a `java.math.BigInteger`, not a [[scala.Int]], and the unscaled value will
 * never be an exact power of ten (in order to facilitate comparison).
 */
private[util] final class SigAndExp(
  val unscaled: BigInteger,
  val scale: BigInteger
) extends BiggerDecimal {
  def isWhole: Boolean = scale.signum != 1
  def isNegativeZero: Boolean = false

  def toBigDecimal: Option[BigDecimal] =
    if (scale.compareTo(BiggerDecimal.MaxInt) <= 0 && scale.compareTo(BiggerDecimal.MinInt) >= 0) {
      Some(new BigDecimal(unscaled, scale.intValue))
    } else None

  def toBigInteger: Option[BigInteger] = if (!isWhole) None else {
    val digits = BigInteger.valueOf(unscaled.toString.length.toLong).subtract(scale)

    if (digits.compareTo(BiggerDecimal.MaxBigIntegerDigits) > 0) None else Some(
      new BigDecimal(unscaled, scale.intValue).toBigInteger
    )
  }

  def toDouble: Double = toBigDecimal.map(_.doubleValue).getOrElse(
    (if (scale.signum == 1) 0.0 else Double.PositiveInfinity) * unscaled.signum
  )

  def toLong: Option[Long] = toBigDecimal.flatMap { d =>
    try {
      Some(d.longValueExact)
    } catch {
      case _: ArithmeticException => None
    }
  }

  def truncateToLong: Long = toDouble.round

  override def equals(that: Any): Boolean = that match {
    case other: SigAndExp => unscaled == other.unscaled && scale == other.scale
    case _ => false
  }

  override def hashCode: Int = scale.hashCode + unscaled.hashCode

  override def toString: String = if (scale == BigInteger.ZERO) unscaled.toString else {
    s"${ unscaled }e${ scale.negate }"
  }
}

final object BiggerDecimal {
  private[util] val MaxBigIntegerDigits: BigInteger = BigInteger.valueOf(1L << 18)

  final val MaxInt: BigInteger = BigInteger.valueOf(Int.MaxValue)
  final val MinInt: BigInteger = BigInteger.valueOf(Int.MinValue)

  private[util] val NegativeZero: BiggerDecimal = new BiggerDecimal {
    final def isWhole: Boolean = true
    final def isNegativeZero: Boolean = true
    final val toBigDecimal: Option[BigDecimal] = Some(BigDecimal.ZERO)
    final val toBigInteger: Option[BigInteger] = Some(BigInteger.ZERO)
    final def toDouble: Double = -0.0
    final val toLong: Option[Long] = Some(truncateToLong)
    final def truncateToLong: Long = 0L

    final override def equals(that: Any): Boolean = that match {
      case other: BiggerDecimal => other.isNegativeZero
      case _ => false
    }
    final override def hashCode: Int = (-0.0).hashCode
    final override def toString: String = "-0"
  }

  @tailrec
  private[this] def removeTrailingZeros(d: BigInteger, depth: Long): (BigInteger, Long) = {
    val divAndRem = d.divideAndRemainder(BigInteger.TEN)

    if (divAndRem(1) == BigInteger.ZERO) removeTrailingZeros(divAndRem(0), depth + 1) else {
      (d, depth)
    }
  }

  def fromBigDecimal(d: BigDecimal): BiggerDecimal = try {
    val noZeros = d.stripTrailingZeros
    new SigAndExp(noZeros.unscaledValue, BigInteger.valueOf(noZeros.scale.toLong))
  } catch {
    case _: ArithmeticException =>
      val (unscaled, zeros) = removeTrailingZeros(d.unscaledValue, 0L)

      new SigAndExp(
        unscaled,
        BigInteger.valueOf(d.scale.toLong).subtract(BigInteger.valueOf(zeros))
      )
  }

  def fromLong(d: Long): BiggerDecimal = fromBigDecimal(BigDecimal.valueOf(d))
  def fromDouble(d: Double): BiggerDecimal = if (java.lang.Double.compare(d, -0.0) == 0) {
    NegativeZero
  } else fromBigDecimal(BigDecimal.valueOf(d))
}
