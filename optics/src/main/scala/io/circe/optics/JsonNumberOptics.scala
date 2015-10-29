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
  val jsonNumberToDouble: Prism[JsonNumber, Double] = Prism[JsonNumber, Double]{ n =>
    val d = n.toDouble
    if (JsonNumber.eqJsonNumber.eqv(JsonDouble(d), n)) Some(d) else None
  }(JsonDouble(_))

  val jsonNumberToBigInt: Prism[JsonNumber, BigInt] =
    Prism[JsonNumber, BigInt](_.toBigInt)(b =>
      JsonBigDecimal(BigDecimal(b, MathContext.UNLIMITED))
    )

  val jsonNumberToLong: Prism[JsonNumber, Long] =
    Prism[JsonNumber, Long](_.toLong)(JsonLong(_))

  val jsonNumberToInt: Prism[JsonNumber, Int] =
    Prism[JsonNumber, Int](_.toInt)(i => JsonLong(i.toLong))

  val jsonNumberToShort: Prism[JsonNumber, Short] =
    Prism[JsonNumber, Short](_.toShort)(s => JsonLong(s.toLong))

  val jsonNumberToByte: Prism[JsonNumber, Byte] =
    Prism[JsonNumber, Byte](_.toByte)(b => JsonLong(b.toLong))

  val jsonNumberToBigDecimal: Iso[JsonNumber, BigDecimal] =
    Iso[JsonNumber, BigDecimal](_.toBigDecimal)(JsonBigDecimal(_))
}
