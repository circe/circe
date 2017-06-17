package io.circe.optics

import io.circe.{ JsonBigDecimal, JsonLong, JsonNumber }
import java.math.{ BigDecimal => JavaBigDecimal, MathContext }
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
  final lazy val jsonNumberBigInt: Prism[JsonNumber, BigInt] = Prism[JsonNumber, BigInt](jn =>
    if (JsonNumberOptics.isNegativeZero(jn)) None else jn.toBigInt
  )(b => JsonBigDecimal(new JavaBigDecimal(b.underlying, MathContext.UNLIMITED)))

  final lazy val jsonNumberLong: Prism[JsonNumber, Long] = Prism[JsonNumber, Long](jn =>
    if (JsonNumberOptics.isNegativeZero(jn)) None else jn.toLong
  )(JsonLong(_))

  final lazy val jsonNumberInt: Prism[JsonNumber, Int] = Prism[JsonNumber, Int](jn =>
    if (JsonNumberOptics.isNegativeZero(jn)) None else jn.toInt
  )(i => JsonLong(i.toLong))

  final lazy val jsonNumberShort: Prism[JsonNumber, Short] = Prism[JsonNumber, Short](jn =>
    if (JsonNumberOptics.isNegativeZero(jn)) None else jn.toShort
  )(s => JsonLong(s.toLong))

  final lazy val jsonNumberByte: Prism[JsonNumber, Byte] = Prism[JsonNumber, Byte](jn =>
    if (JsonNumberOptics.isNegativeZero(jn)) None else jn.toByte
  )(b => JsonLong(b.toLong))

  final lazy val jsonNumberBigDecimal: Prism[JsonNumber, BigDecimal] =
    Prism[JsonNumber, BigDecimal](jn =>
      if (JsonNumberOptics.isNegativeZero(jn)) None else jn.toBigDecimal
    )(b => JsonBigDecimal(b.underlying))
}

final object JsonNumberOptics extends JsonNumberOptics {
  private[optics] def isNegativeZero(jn: JsonNumber): Boolean = jn.toBiggerDecimal.isNegativeZero
}
