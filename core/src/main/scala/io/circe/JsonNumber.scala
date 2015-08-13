package io.circe

import java.math.{ BigDecimal => JBigDecimal, MathContext }
import scala.util.matching.Regex

/**
 * A JSON number with optimization by cases.
 */
sealed abstract class JsonNumber extends Serializable {
  /**
   * Return this number as a [[scala.math.BigDecimal]].
   */
  def toBigDecimal: BigDecimal

  /**
   * Convert this number to its best [[scala.Double]] approximation.
   *
   * Anything over `Double.MaxValue` will be rounded to `Double.PositiveInfinity` and anything below
   * `Double.MinValue` is rounded to `Double.NegativeInfinity`.
   */
  def toDouble: Double

  /**
   * Return this number as a [[scala.math.BigInt]] if it is an integer.
   */
  def toBigInt: Option[BigInt] = {
    val n = toBigDecimal
    if (n.isWhole) Some(n.toBigInt) else None
  }

  /**
   * Return this number as a [[scala.Byte]] if it's a valid [[scala.Byte]].
   */
  def toByte: Option[Byte] = toLong.flatMap { n =>
    val asByte: Byte = n.toByte
    if (n == asByte) Some(asByte) else None
  }

  /**
   * Return this number as a [[scala.Short]] if it's a valid [[scala.Short]].
   */
  def toShort: Option[Short] = toLong.flatMap { n =>
    val asShort: Short = n.toShort
    if (n == asShort) Some(asShort) else None
  }

  /**
   * Return this number as an [[scala.Int]] if it's a valid [[scala.Int]].
   */
  def toInt: Option[Int] = toLong.flatMap { n =>
    val asInt: Int = n.toInt
    if (n == asInt) Some(asInt) else None
  }

  /**
   * Return this number as a [[scala.Long]] if it's a valid [[scala.Long]].
   */
  def toLong: Option[Long]

  /**
   * Truncate the number to a [[scala.math.BigInt]].
   *
   * Truncation means that we round toward zero to the closest [[scala.math.BigInt]].
   */
  def truncateToBigInt: BigInt =  toBigDecimal.toBigInt

  /**
   * Truncate the number to a [[scala.Byte]].
   *
   * Truncation means that we round toward zero to the closest valid [[scala.Byte]]. If the number
   * is `1e99`, for example, this will return `Byte.MaxValue`.
   */
  def truncateToByte: Byte = {
    val asInt: Int = truncateToInt
    if (asInt > Byte.MaxValue) {
      Byte.MaxValue
    } else if (asInt < Byte.MinValue) {
      Byte.MinValue
    } else asInt.toByte
  }

  /**
   * Truncate the number to a [[scala.Short]].
   *
   * Truncation means that we round toward zero to the closest valid [[scala.Short]]. If the number
   * is `1e99`, for example, this will return `Short.MaxValue`.
   */
  def truncateToShort: Short = {
    val asInt: Int = truncateToInt
    if (asInt > Short.MaxValue) {
      Short.MaxValue
    } else if (asInt < Short.MinValue) {
      Short.MinValue
    } else asInt.toShort
  }

  /**
   * Truncate the number to an [[scala.Int]].
   *
   * Truncation means that we round toward zero to the closest valid [[scala.Int]]. If the number is
   * `1e99`, for example, this will return `Int.MaxValue`.
   */
  def truncateToInt: Int = toDouble.toInt

  /**
   * Truncate the number to a [[scala.Long]].
   *
   * Truncation means that we round toward zero to the closest valid [[scala.Long]]. If the number
   * is `1e99`, for example, this will return `Long.MaxValue`.
   */
  def truncateToLong: Long

  /**
   * Return `true` if and only if this number wraps a [[scala.Double]] and it is `Double.NaN`.
   */
  protected def isNaN: Boolean = false

  /**
   * Return `true` if and only if this number wraps a [[scala.Double]] and is either
   * `Double.NegativeInfinity` or `Double.PositiveInfinity`.
   */
  protected def isInfinity: Boolean = false

  /**
   * Return true if this is a valid real number (i.e. not infinity or `Double.NaN`).
   */
  protected def isReal: Boolean = !(isNaN || isInfinity)

  /**
   * Construct a JSON number if this is a valid JSON number.
   */
  def asJson: Option[Json] = if (isReal) Some(Json.fromJsonNumber(this)) else None

  /**
   * Construct a JSON number if this is a valid JSON number and a JSON null otherwise.
   *
   * This matches the behaviour of most browsers, but it is a lossy operation as you can no longer
   * distinguish between `Double.NaN` and infinity.
   */
  def asJsonOrNull: Json = asJson.getOrElse(Json.empty)

  /**
   * Construct a JSON number if this is a valid JSON number and a JSON string otherwise.
   *
   * This allows a [[scala.Double]] to be losslessly encoded, but it is likely to need custom
   * handling for interoperability with other JSON systems.
   */
  def asJsonOrString: Json = asJson.getOrElse(Json.string(toString))

  /**
   * Force this [[JsonNumber]] into a [[JsonDecimal]] by using the underlying `toString`.
   *
   * @note Unsafe if `isReal` is `false`.
   */
  private def toJsonDecimal: JsonDecimal = this match {
    case n @ JsonDecimal(_) => n
    case JsonBigDecimal(n) => JsonDecimal(n.toString)
    case JsonLong(n) => JsonDecimal(n.toString)
    case JsonDouble(n) => JsonDecimal(n.toString)
  }

  /**
   * Type-safe equality for [[JsonNumber]].
   */
  def ===(that: JsonNumber): Boolean =
    if (this.isReal && that.isReal) {
      (this, that) match {
        case (a @ JsonDecimal(_), b) => a.normalized == b.toJsonDecimal.normalized
        case (a, b @ JsonDecimal(_)) => a.toJsonDecimal.normalized == b.normalized
        case (JsonLong(x), JsonLong(y)) => x == y
        case (JsonDouble(x), JsonLong(y)) => x == y
        case (JsonLong(x), JsonDouble(y)) => y == x
        case (JsonDouble(x), JsonDouble(y)) => x == y
        case (a, b) => a.toBigDecimal == b.toBigDecimal
      }
    } else {
      this.toDouble == that.toDouble
    }

  /**
   * Type-safe inequality for [[JsonNumber]].
   */
  def =!=(that: JsonNumber): Boolean = !(this === that)

  /**
   * Universal equality derived from our type-safe equality.
   */
  override def equals(that: Any): Boolean =
    that match {
      case that: JsonNumber => this === that
      case _ => false
    }

  /**
   * Hashing that is consistent with our universal equality.
   */
  override def hashCode: Int =
    if (isReal) toJsonDecimal.normalized.hashCode else toDouble.hashCode
}

/**
 * Represent a valid JSON number as a `String`.
 *
 * Unfortunately there is no type in the Scala standard library which can represent all valid JSON
 * decimal numbers, since the exponent may be larger than an [[scala.Int]], but such a number can
 * still be round-tripped through a string representation. We lazily parse the string to a
 * [[scala.math.BigDecimal]] or a [[scala.Double]] on demand.
 */
private[circe] final case class JsonDecimal(value: String) extends JsonNumber {
  lazy val toBigDecimal: BigDecimal = BigDecimal(value, MathContext.UNLIMITED)
  lazy val toDouble: Double = value.toDouble
  override def toString: String = value

  def toLong: Option[Long] = {
    val asBigDecimal: BigDecimal = toBigDecimal
    if (asBigDecimal.isValidLong) Some(asBigDecimal.toLong) else None
  }

  def truncateToLong: Long = {
    val asBigDecimal: BigDecimal = toBigDecimal
    if (asBigDecimal >= Long.MaxValue) {
      Long.MaxValue
    } else if (asBigDecimal <= Long.MinValue) {
      Long.MinValue
    } else asBigDecimal.toLong
  }

  /**
   * Return a *normalized* version of this decimal number.
   *
   * Since [[scala.math.BigDecimal]] cannot represent all valid JSON values exactly (due to the
   * exponent being limited to an [[scala.Int]]), this method provides a normalized number that can
   * be used to compare for equality.
   *
   * The first part of the return value is the exponent used to scale the second part back to the
   * original value represented by this number. The [[scala.math.BigDecimal]] will always either be
   * zero or a number with exactly one decimal digit to the right of the decimal point. If the
   * [[scala.math.BigDecimal]] value is zero, then the exponent will always be zero as well.
   */
  def normalized: (BigInt, BigDecimal) = {
    val JsonNumber.JsonNumberRegex(negative, intStr, decStr, expStr) = value

    def scaledValue(shift: Int): BigDecimal = {
      val unscaled: JBigDecimal =
        if (decStr == null) {
          new JBigDecimal(intStr, MathContext.UNLIMITED)
        } else {
          new JBigDecimal(s"$intStr.$decStr", MathContext.UNLIMITED)
        }

      val scaled: JBigDecimal = unscaled.movePointLeft(shift)

      BigDecimal(if (negative != null) scaled.negate else scaled)
    }

    def unscaledExponent: BigInt =
      if (expStr == null) {
        BigInt(0)
      } else if (expStr.charAt(0) == '+') {
        BigInt(expStr.substring(1))
      } else {
        BigInt(expStr)
      }

    if (intStr != "0") {
      val shift = intStr.length + 1
      (unscaledExponent + BigInt(shift), scaledValue(shift))
    } else if (decStr != null) {
      var i = 0
      while (i < decStr.length && decStr.charAt(i) == '0') i += 1

      if (i < decStr.length) {
        val shift = -i - 1
        (unscaledExponent + BigInt(shift), scaledValue(shift))
      } else (BigInt(0), BigDecimal(0))
    } else (BigInt(0), BigDecimal(0))
  }
}

/**
 * Represent a valid JSON number as a [[scala.math.BigDecimal]].
 */
private[circe] final case class JsonBigDecimal(value: BigDecimal) extends JsonNumber {
  def toBigDecimal: BigDecimal = value
  def toDouble: Double = value.toDouble
  override def toString: String = value.toString
  def toLong: Option[Long] = if (value.isValidLong) Some(value.toLong) else None

  def truncateToLong: Long =
    if (value > Long.MaxValue) {
      Long.MaxValue
    } else if (value < Long.MinValue) {
      Long.MinValue
    } else value.toLong
}

/**
 * Represent a valid JSON number as a [[scala.Long]].
 */
private[circe] final case class JsonLong(value: Long) extends JsonNumber {
  def toBigDecimal: BigDecimal = BigDecimal(value)
  def toDouble: Double = value.toDouble
  override def toString: String = value.toString
  def toLong: Option[Long] = Some(value)
  def truncateToLong: Long = value
}

/**
 * Represent a valid JSON number as a [[scala.Double]].
 */
private[circe] final case class JsonDouble(value: Double) extends JsonNumber {
  def toBigDecimal: BigDecimal = BigDecimal(value)
  def toDouble: Double = value
  override def toString: String = value.toString

  def toLong: Option[Long] = {
    val asLong: Long = value.toLong
    if (asLong.toDouble == value) Some(asLong) else None
  }

  def truncateToLong: Long = value.toLong
  override def isNaN = value.isNaN
  override def isInfinity = value.isInfinity
}

/**
 * Constructors, type class instances, and other utilities for [[JsonNumber]].
 */
object JsonNumber {
  /**
   * Return a `JsonNumber` whose value is the valid JSON number in `value`.
   *
   * @note This value is ''not'' verified to be a valid JSON string. It is assumed that `value` is a
   * valid JSON number, according to the JSON specification. If the value is invalid the behavior is
   * undefined. This operation is provided for use in situations where the validity of the input has
   * already been verified.
   */
  def unsafeDecimal(value: String): JsonNumber = JsonDecimal(value)

  /**
   * Parse a JSON number from a `String`.
   *
   * A string is valid if it conforms to the grammar in section 2.4 of the
   * [[http://www.ietf.org/rfc/rfc4627.txt JSON specification]]. If it is valid, then the number is
   * returned in a `Some`. Otherwise the number is invalid and `None` is returned.
   *
   * @param value a JSON number encoded as a string
   * @return a JSON number if the string is valid
   */
  def fromString(value: String): Option[JsonNumber] = {
    // Span over [0-9]*
    def digits(index: Int): Int = {
      if (index >= value.length) value.length
      else {
        val char = value.charAt(index)
        if (char >= '0' && char <= '9') digits(index + 1)
        else index
      }
    }

    // Verify [0-9]+
    def digits1(index: Int): Int = {
      val end = digits(index)
      if (end == index) -1 else end
    }

    // Verify 0 | [1-9][0-9]*
    def natural(index: Int): Int = {
      if (index >= value.length) -1
      else {
        val char = value.charAt(index)
        if (char == '0') index + 1
        else if (char >= '1' && char <= '9') digits(index + 1)
        else index
      }
    }

    // Verify -?(0 | [1-9][0-9]*)
    def integer(index: Int): Int = {
      if (index >= value.length) -1
      else if (value.charAt(index) == '-') natural(index + 1)
      else natural(index)
    }

    // Span .[0-9]+
    def decimal(index: Int): Int = {
      if (index < 0 || index >= value.length) index
      else if (value.charAt(index) == '.') digits1(index + 1)
      else index
    }

    // Span e[-+]?[0-9]+
    def exponent(index: Int): Int = {
      if (index < 0 || index >= value.length) index
      else {
        val e = value.charAt(index)
        if (e == 'e' || e == 'E') {
          val index0 = index + 1
          if (index0 < value.length) {
            val sign = value.charAt(index0)
            if (sign == '+' || sign == '-') digits1(index0 + 1)
            else digits1(index0)
          } else {
            -1
          }
        } else {
          -1
        }
      }
    }

    val intIndex = integer(0)
    val decIndex = decimal(intIndex)
    val expIndex = exponent(decIndex)

    val invalid =
      (expIndex != value.length) ||
      (intIndex == 0) ||
      (intIndex == -1) ||
      (decIndex == -1)

    // Assuming the number is an integer, does it fit in a Long?
    def isLong: Boolean = {
      val upperBound = if (value.charAt(0) == '-') MinLongString else MaxLongString
      (value.length < upperBound.length) ||
        ((value.length == upperBound.length) &&
          value.compareTo(upperBound) <= 0)
    }

    if (invalid) {
      None
    } else if (intIndex == expIndex && isLong) {
      Some(JsonLong(value.toLong))
    } else {
      Some(JsonDecimal(value))
    }
  }

  private[this] val MaxLongString: String = Long.MaxValue.toString
  private[this] val MinLongString: String = Long.MinValue.toString

  /**
   * A regular expression that can match a valid JSON number.
   *
   * This has four match groups:
   *
   *  1. The optional negative sign.
   *  2. The integer part.
   *  3. The fractional part without the leading period.
   *  4. The exponent part without the leading 'e', but with an optional leading '+' or '-'.
   *
   * The negative sign, fractional part and exponent part are optional matches and may be `null`.
   */
  val JsonNumberRegex: Regex = """(-)?((?:[1-9][0-9]*|0))(?:\.([0-9]+))?(?:[eE]([-+]?[0-9]+))?""".r
}
