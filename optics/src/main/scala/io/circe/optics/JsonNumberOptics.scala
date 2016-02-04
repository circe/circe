package io.circe.optics

import io.circe.{ JsonBigDecimal, JsonDouble, JsonLong, JsonNumber }
import java.math.MathContext
import monocle.Prism

/**
 * Optics instances for [[io.circe.JsonObject]].
 *
 * @author Sean Parsons
 * @author Travis Brown
 */
trait JsonNumberOptics {
  final lazy val jsonNumberDouble: Prism[JsonNumber, Double] = Prism[JsonNumber, Double]{ n =>
    val d = n.toDouble
    if (JsonNumber.eqJsonNumber.eqv(JsonDouble(d), n)) Some(d) else None
  }(JsonDouble(_))

  final lazy val jsonNumberBigInt: Prism[JsonNumber, BigInt] = Prism[JsonNumber, BigInt](jn =>
    if (JsonNumberOptics.isNegativeZero(jn)) None else jn.toBigInt
  )(b => JsonBigDecimal(BigDecimal(b, MathContext.UNLIMITED)))

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
    )(JsonBigDecimal(_))
}

final object JsonNumberOptics extends JsonNumberOptics {
  private[optics] def isNegativeZero(jn: JsonNumber): Boolean = jn.toBiggerDecimal.isNegativeZero
}
