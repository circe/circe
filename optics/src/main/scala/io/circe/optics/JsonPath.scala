package io.circe.optics

import io.circe.{ Decoder, Encoder, Json }
import io.circe.optics.JsonOptics._, io.circe.optics.JsonObjectOptics._
import monocle.{ Optional, Prism }
import monocle.function.index
import monocle.std.list._
import scala.language.dynamics

case class JsonPath(opt: Optional[Json, Json]) extends Dynamic {
  def selectDynamic(field: String): JsonPath =
    JsonPath(opt.composePrism(jsonObject).composeOptional(index(field)))

  def at(i: Int): JsonPath =
    JsonPath(opt.composePrism(jsonArray).composeOptional(index(i)))

  def as[A](implicit decode: Decoder[A], encode: Encoder[A]): Optional[Json, A] =
    opt.composePrism(
      Prism((j: Json) => decode.decodeJson(j).toOption)(encode(_))
    )

  def `*`: Optional[Json, Json] = opt
}

object JsonPath {
  val root: JsonPath = JsonPath(Optional.id)
}
