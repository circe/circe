package io.circe.optics

import io.circe.{ Json, JsonNumber, JsonObject }
import io.circe.optics.JsonNumberOptics._
import monocle.Prism

/**
 * Optics instances for [[io.circe.Json]].
 *
 * @author Sean Parsons
 * @author Travis Brown
 */
trait JsonOptics {
  lazy val jsonBoolean: Prism[Json, Boolean] = Prism[Json, Boolean](_.asBoolean)(Json.bool)
  lazy val jsonBigDecimal: Prism[Json, BigDecimal] = jsonNumber.composeIso(jsonNumberToBigDecimal)
  lazy val jsonDouble: Prism[Json, Double] = jsonNumber.composePrism(jsonNumberToDouble)
  lazy val jsonBigInt: Prism[Json, BigInt] = jsonNumber.composePrism(jsonNumberToBigInt)
  lazy val jsonLong: Prism[Json, Long] = jsonNumber.composePrism(jsonNumberToLong)
  lazy val jsonInt: Prism[Json, Int] = jsonNumber.composePrism(jsonNumberToInt)
  lazy val jsonShort: Prism[Json, Short] = jsonNumber.composePrism(jsonNumberToShort)
  lazy val jsonByte: Prism[Json, Byte] = jsonNumber.composePrism(jsonNumberToByte)
  lazy val jsonString: Prism[Json, String] = Prism[Json, String](_.asString)(Json.string)
  lazy val jsonNumber: Prism[Json, JsonNumber] =
    Prism[Json, JsonNumber](_.asNumber)(Json.fromJsonNumber)
  lazy val jsonObject: Prism[Json, JsonObject] =
    Prism[Json, JsonObject](_.asObject)(Json.fromJsonObject)
  lazy val jsonArray: Prism[Json, List[Json]] = Prism[Json, List[Json]](_.asArray)(Json.fromValues)
}

object JsonOptics extends JsonOptics
