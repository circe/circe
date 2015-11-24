package io.circe.optics

import io.circe.{ JsonBigDecimal, JsonDouble, JsonLong, JsonNumber }
import java.math.MathContext
import monocle.{ Iso, Prism }

/**
 * Optics instances for [[io.circe.JsonObject]].
 *
 * @author Sean Parsons
 * @author Travis Brown
 */
trait JsonNumberOptics {
  lazy val jsonNumberDouble: Prism[JsonNumber, Double] = Prism[JsonNumber, Double]{ n =>
    val d = n.toDouble
    if (JsonNumber.eqJsonNumber.eqv(JsonDouble(d), n)) Some(d) else None
  }(JsonDouble(_))

  lazy val jsonNumberBigInt: Prism[JsonNumber, BigInt] =
    Prism[JsonNumber, BigInt](_.toBigInt)(b =>
      JsonBigDecimal(BigDecimal(b, MathContext.UNLIMITED))
    )

  lazy val jsonNumberLong: Prism[JsonNumber, Long] =
    Prism[JsonNumber, Long](_.toLong)(JsonLong(_))

  lazy val jsonNumberInt: Prism[JsonNumber, Int] =
    Prism[JsonNumber, Int](_.toInt)(i => JsonLong(i.toLong))

  lazy val jsonNumberShort: Prism[JsonNumber, Short] =
    Prism[JsonNumber, Short](_.toShort)(s => JsonLong(s.toLong))

  lazy val jsonNumberByte: Prism[JsonNumber, Byte] =
    Prism[JsonNumber, Byte](_.toByte)(b => JsonLong(b.toLong))

  lazy val jsonNumberBigDecimal: Iso[JsonNumber, BigDecimal] =
    Iso[JsonNumber, BigDecimal](_.toBigDecimal)(JsonBigDecimal(_))
}

object JsonNumberOptics extends JsonNumberOptics
