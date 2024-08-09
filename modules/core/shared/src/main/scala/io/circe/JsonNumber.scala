/*
 * Copyright 2024 circe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.circe

import cats.kernel.Eq
import io.circe.numbers.BiggerDecimal

import java.io.Serializable
import java.lang.StringBuilder
import java.math.{ BigDecimal => JavaBigDecimal }

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
   * Universal equality derived from our type-safe equality.
   */
  override final def equals(that: Any): Boolean = that match {
    case that: JsonNumber => JsonNumber.eqJsonNumber.eqv(this, that)
    case _                => false
  }

  /**
   * Hashing that is consistent with our universal equality.
   */
  override final def hashCode: Int = toBiggerDecimal.hashCode

  private[circe] def appendToStringBuilder(builder: StringBuilder): Unit
}

private sealed abstract class BiggerDecimalJsonNumber(input: String) extends JsonNumber {
  final def toBigInt: Option[BigInt] = toBiggerDecimal.toBigInteger.map(BigInt(_))
  final def toBigDecimal: Option[BigDecimal] =
    toBiggerDecimal.toBigDecimal.map(value =>
      if (value == JavaBigDecimal.ZERO) BigDecimal(JavaBigDecimal.ZERO)
      else {
        try BigDecimal(input)
        catch {
          case _: NumberFormatException => value
        }
      }
    )

  final def toLong: Option[Long] = toBiggerDecimal.toLong
  override final def toString: String = input
  private[circe] final def appendToStringBuilder(builder: StringBuilder): Unit = builder.append(input)
}

/**
 * Represent a valid JSON number as a `String`.
 */
private[circe] final case class JsonDecimal(input: String) extends BiggerDecimalJsonNumber(input) {
  private[circe] lazy val toBiggerDecimal: BiggerDecimal = {
    val result = BiggerDecimal.parseBiggerDecimalUnsafe(input)

    if (result.eq(null)) {
      throw new NumberFormatException("For input string \"" + input + "\"")
    } else result
  }

  final def toDouble: Double = java.lang.Double.parseDouble(input)
  final def toFloat: Float = java.lang.Float.parseFloat(input)
}

private[circe] final case class JsonBiggerDecimal(value: BiggerDecimal, input: String)
    extends BiggerDecimalJsonNumber(input) {
  private[circe] def toBiggerDecimal: BiggerDecimal = value
  final def toDouble: Double = toBiggerDecimal.toDouble
  final def toFloat: Float = toBiggerDecimal.toFloat
}

/**
 * Represent a valid JSON number as a `java.math.BigDecimal`.
 */
private[circe] final case class JsonBigDecimal(value: JavaBigDecimal) extends JsonNumber {
  private[circe] def toBiggerDecimal: BiggerDecimal = BiggerDecimal.fromBigDecimal(value)
  final def toBigDecimal: Option[BigDecimal] = Some(new BigDecimal(value))
  final def toBigInt: Option[BigInt] = toBiggerDecimal.toBigInteger.map(BigInt(_))
  final def toDouble: Double = value.doubleValue
  final def toFloat: Float = value.floatValue
  final def toLong: Option[Long] = toBiggerDecimal.toLong
  override final def toString: String = value.toString
  private[circe] def appendToStringBuilder(builder: StringBuilder): Unit = builder.append(value.toString)
}

/**
 * Represent a valid JSON number as a [[scala.Long]].
 */
private[circe] final case class JsonLong(value: Long) extends JsonNumber {
  private[circe] def toBiggerDecimal: BiggerDecimal = BiggerDecimal.fromLong(value)
  final def toBigDecimal: Option[BigDecimal] = Some(BigDecimal(value))
  final def toBigInt: Option[BigInt] = Some(BigInt(value))
  final def toDouble: Double = value.toDouble
  final def toFloat: Float = value.toFloat
  final def toLong: Option[Long] = Some(value)
  override final def toString: String = java.lang.Long.toString(value)
  private[circe] def appendToStringBuilder(builder: StringBuilder): Unit = builder.append(value)
}

/**
 * Represent a valid JSON number as a [[scala.Double]].
 */
private[circe] final case class JsonDouble(value: Double) extends JsonNumber {
  private[circe] def toBiggerDecimal: BiggerDecimal = BiggerDecimal.fromDoubleUnsafe(value)
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
}

/**
 * Represent a valid JSON number as a [[scala.Float]].
 */
private[circe] final case class JsonFloat(value: Float) extends JsonNumber {
  private[circe] def toBiggerDecimal: BiggerDecimal = BiggerDecimal.fromFloat(value)
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
}

/**
 * Constructors, type class instances, and other utilities for [[JsonNumber]].
 */
object JsonNumber {

  /**
   * Constant pool of integer numbers between -128 to 127, similar to the JVM's constant pool for integers.
   */
  private[this] val jsonLongConstantPool: Array[Json.JNumber] =
    Array.tabulate(256)(x => Json.JNumber(JsonLong((x - 128).toLong)))

  private[circe] final def fromLong(value: Long): Json.JNumber = {
    val idx = value + 128
    if (idx >= 0 && idx < jsonLongConstantPool.length) jsonLongConstantPool.apply(idx.toInt)
    else Json.JNumber(JsonLong(value))
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

  /**
   * Return a `JsonNumber` whose value is the valid integral JSON number in `value`.
   *
   * @note This value is ''not'' verified to be a valid JSON string. It is assumed that `value` is a
   * valid JSON number, according to the JSON specification. If the value is invalid the behavior is
   * undefined. This operation is provided for use in situations where the validity of the input has
   * already been verified.
   */
  final def fromIntegralStringUnsafe(value: String): JsonNumber =
    if (!BiggerDecimal.integralIsValidLong(value)) JsonDecimal(value)
    else {
      val longValue = java.lang.Long.parseLong(value)

      if (value.charAt(0) == '-' && longValue == 0L) JsonDecimal(value) else fromLong(longValue).value
    }

  final def fromString(value: String): Option[JsonNumber] = {
    val result = BiggerDecimal.parseBiggerDecimalUnsafe(value)

    if (result.eq(null)) None else Some(JsonBiggerDecimal(result, value))
  }

  private[this] val bigDecimalMinLong: JavaBigDecimal = new JavaBigDecimal(Long.MinValue)
  private[this] val bigDecimalMaxLong: JavaBigDecimal = new JavaBigDecimal(Long.MaxValue)

  private[circe] def bigDecimalIsWhole(value: JavaBigDecimal): Boolean =
    value.signum == 0 || value.scale <= 0 || value.stripTrailingZeros.scale <= 0

  private[circe] def bigDecimalIsValidLong(value: JavaBigDecimal): Boolean =
    bigDecimalIsWhole(value) && value.compareTo(bigDecimalMinLong) >= 0 && value.compareTo(bigDecimalMaxLong) <= 0

  implicit final val eqJsonNumber: Eq[JsonNumber] = Eq.instance {
    case (JsonLong(x), JsonLong(y))             => x == y
    case (JsonDouble(x), JsonDouble(y))         => java.lang.Double.compare(x, y) == 0
    case (JsonFloat(x), JsonFloat(y))           => java.lang.Float.compare(x, y) == 0
    case (JsonBigDecimal(x), JsonBigDecimal(y)) => x.compareTo(y) == 0
    case (a, b)                                 => a.toBiggerDecimal == b.toBiggerDecimal
  }
}
