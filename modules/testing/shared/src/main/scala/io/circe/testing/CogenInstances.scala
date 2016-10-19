package io.circe.testing

import io.circe.{ DecodingFailure, Json, JsonNumber, JsonObject }
import org.scalacheck.Cogen

private[testing] trait CogenInstances {
  implicit val cogenDecodingFailure: Cogen[DecodingFailure] = Cogen((_: DecodingFailure).hashCode.toLong)
  implicit val cogenJson: Cogen[Json] = Cogen((_: Json).hashCode.toLong)
  implicit val cogenJsonNumber: Cogen[JsonNumber] = Cogen((_: JsonNumber).hashCode.toLong)
  implicit val cogenJsonObject: Cogen[JsonObject] = Cogen((_: JsonObject).hashCode.toLong)
}
