package io.circe.optics

import io.circe.numbers.JsonNumber
import monocle.Prism

/**
 * Optics instances for [[io.circe.JsonObject]].
 *
 * Note that the prisms for integral types will fail on [[io.circe.JsonNumber]] values representing
 * negative zero, since this would make them unlawful.
 *
 * @author Sean Parsons
 * @author Travis Brown
 */
trait JsonNumberOptics {
  final lazy val jsonNumberBigDecimal: Prism[JsonNumber, BigDecimal] = Prism[JsonNumber, BigDecimal](jn =>
    if (jn.isNegativeZero) None else jn.toBigDecimal.map(new BigDecimal(_))
  )(b => JsonNumber.fromBigDecimal(b.underlying))

  final lazy val jsonNumberBigInt: Prism[JsonNumber, BigInt] = Prism[JsonNumber, BigInt](jn =>
    if (jn.isNegativeZero) None else jn.toBigInteger.map(new BigInt(_))
  )(b => JsonNumber.fromBigInteger(b.underlying))

  final lazy val jsonNumberLong: Prism[JsonNumber, Long] = Prism[JsonNumber, Long](jn =>
    if (jn.isNegativeZero) None else jn.toLong
  )(JsonNumber.fromLong)

  final lazy val jsonNumberInt: Prism[JsonNumber, Int] = Prism[JsonNumber, Int](jn =>
    if (jn.isNegativeZero) None else jn.toInt
  )(i => JsonNumber.fromLong(i.toLong))

  final lazy val jsonNumberShort: Prism[JsonNumber, Short] = Prism[JsonNumber, Short](jn =>
    if (jn.isNegativeZero) None else jn.toShort
  )(s => JsonNumber.fromLong(s.toLong))

  final lazy val jsonNumberByte: Prism[JsonNumber, Byte] = Prism[JsonNumber, Byte](jn =>
    if (jn.isNegativeZero) None else jn.toByte
  )(b => JsonNumber.fromLong(b.toLong))
}

object JsonNumberOptics extends JsonNumberOptics