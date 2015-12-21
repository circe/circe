package io.circe.optics

import io.circe.{ Decoder, Encoder, Json }
import io.circe.optics.JsonObjectOptics._
import io.circe.optics.JsonOptics._
import monocle.{ Optional, Prism }
import monocle.function.Index.index
import monocle.std.list._
import scala.language.dynamics

case class JsonPath(json: Optional[Json, Json]) extends Dynamic {
  def selectDynamic(field: String): JsonPath =
    JsonPath(json.composePrism(jsonObject).composeOptional(index(field)))

  def at(i: Int): JsonPath =
    JsonPath(json.composePrism(jsonArray).composeOptional(index(i)))

  /**
   * Decode a value at the current location.
   *
   * Note that this operation is not lawful, since decoding is not injective (as noted by Julien
   * Truffaut). It is provided here for convenience, but may change in future versions.
   */
  def as[A](implicit decode: Decoder[A], encode: Encoder[A]): Optional[Json, A] =
    json.composePrism(
      Prism((j: Json) => decode.decodeJson(j).toOption)(encode(_))
    )
}

object JsonPath {
  val root: JsonPath = JsonPath(Optional.id)
}
