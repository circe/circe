package io.circe.optics

import io.circe.{ Json, JsonNumber, JsonObject }
import monocle.Prism

/**
 * Optics instances for [[io.circe.Json]].
 *
 * @author Sean Parsons
 * @author Travis Brown
 */
trait JsonOptics {
  val jsonBoolean: Prism[Json, Boolean] = Prism[Json, Boolean](_.asBoolean)(Json.bool)
  val jsonBigDecimal: Prism[Json, BigDecimal] = jsonNumber.composeIso(jsonNumberToBigDecimal)
  val jsonDouble: Prism[Json, Double] = jsonNumber.composePrism(jsonNumberToDouble)
  val jsonBigInt: Prism[Json, BigInt] = jsonNumber.composePrism(jsonNumberToBigInt)
  val jsonLong: Prism[Json, Long] = jsonNumber.composePrism(jsonNumberToLong)
  val jsonInt: Prism[Json, Int] = jsonNumber.composePrism(jsonNumberToInt)
  val jsonShort: Prism[Json, Short] = jsonNumber.composePrism(jsonNumberToShort)
  val jsonByte: Prism[Json, Byte] = jsonNumber.composePrism(jsonNumberToByte)
  val jsonString: Prism[Json, String] = Prism[Json, String](_.asString)(Json.string)
  val jsonNumber: Prism[Json, JsonNumber] =
    Prism[Json, JsonNumber](_.asNumber)(Json.fromJsonNumber)
  val jsonObject: Prism[Json, JsonObject] =
    Prism[Json, JsonObject](_.asObject)(Json.fromJsonObject)
}
