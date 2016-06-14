package io.circe.optics

import io.circe.{ Decoder, Encoder, Json, JsonNumber, JsonObject }
import io.circe.optics.JsonObjectOptics._
import io.circe.optics.JsonOptics._
import monocle.{ Optional, Prism, Traversal }
import monocle.function.{ At, FilterIndex, Index }
import monocle.std.list._
import scala.language.dynamics

final case class JsonPath(json: Optional[Json, Json]) extends Dynamic {
  final def `null`: Optional[Json, Unit] = json composePrism jsonNull
  final def boolean: Optional[Json, Boolean] = json composePrism jsonBoolean
  final def byte: Optional[Json, Byte] = json composePrism jsonByte
  final def short: Optional[Json, Short] = json composePrism jsonShort
  final def int: Optional[Json, Int] = json composePrism jsonInt
  final def long: Optional[Json, Long] = json composePrism jsonLong
  final def bigInt: Optional[Json, BigInt] = json composePrism jsonBigInt
  final def double: Optional[Json, Double] = json composePrism jsonDouble
  final def bigDecimal: Optional[Json, BigDecimal] = json composePrism jsonBigDecimal
  final def number: Optional[Json, JsonNumber] = json composePrism jsonNumber
  final def string: Optional[Json, String] = json composePrism jsonString
  final def arr: Optional[Json, List[Json]] = json composePrism jsonArray
  final def obj: Optional[Json, JsonObject] = json composePrism jsonObject

  final def at(field: String): Optional[Json, Option[Json]] =
    json.composePrism(jsonObject).composeLens(At.at(field))

  final def selectDynamic(field: String): JsonPath =
    JsonPath(json.composePrism(jsonObject).composeOptional(Index.index(field)))

  final def index(i: Int): JsonPath =
    JsonPath(json.composePrism(jsonArray).composeOptional(Index.index(i)))

  final def each: JsonTraversalPath =
    JsonTraversalPath(json composeTraversal jsonDescendants)

  final def arrFilter(p: Int => Boolean): JsonTraversalPath =
    JsonTraversalPath(arr composeTraversal FilterIndex.filterIndex(p))

  final def objFilter(p: String => Boolean): JsonTraversalPath =
    JsonTraversalPath(obj composeTraversal FilterIndex.filterIndex(p))

  /**
   * Decode a value at the current location.
   *
   * Note that this operation is not lawful, since decoding is not injective (as noted by Julien
   * Truffaut). It is provided here for convenience, but may change in future versions.
   */
  final def as[A](implicit decode: Decoder[A], encode: Encoder[A]): Optional[Json, A] =
    json.composePrism(
      Prism((j: Json) => decode.decodeJson(j).toOption)(encode(_))
    )
}

final object JsonPath {
  final val root: JsonPath = JsonPath(Optional.id)
}

final case class JsonTraversalPath(json: Traversal[Json, Json]) extends Dynamic {
  final def `null`: Traversal[Json, Unit] = json composePrism jsonNull
  final def boolean: Traversal[Json, Boolean] = json composePrism jsonBoolean
  final def byte: Traversal[Json, Byte] = json composePrism jsonByte
  final def short: Traversal[Json, Short] = json composePrism jsonShort
  final def int: Traversal[Json, Int] = json composePrism jsonInt
  final def long: Traversal[Json, Long] = json composePrism jsonLong
  final def bigInt: Traversal[Json, BigInt] = json composePrism jsonBigInt
  final def double: Traversal[Json, Double] = json composePrism jsonDouble
  final def bigDecimal: Traversal[Json, BigDecimal] = json composePrism jsonBigDecimal
  final def number: Traversal[Json, JsonNumber] = json composePrism jsonNumber
  final def string: Traversal[Json, String] = json composePrism jsonString
  final def arr: Traversal[Json, List[Json]] = json composePrism jsonArray
  final def obj: Traversal[Json, JsonObject] = json composePrism jsonObject

  final def at(field: String): Traversal[Json, Option[Json]] =
    json.composePrism(jsonObject).composeLens(At.at(field))

  final def selectDynamic(field: String): JsonTraversalPath =
    JsonTraversalPath(json.composePrism(jsonObject).composeOptional(Index.index(field)))

  final def index(i: Int): JsonTraversalPath =
    JsonTraversalPath(json.composePrism(jsonArray).composeOptional(Index.index(i)))

  final def each: JsonTraversalPath =
    JsonTraversalPath(json composeTraversal jsonDescendants)

  final def arrFilter(p: Int => Boolean): JsonTraversalPath =
    JsonTraversalPath(arr composeTraversal FilterIndex.filterIndex(p))

  final def objFilter(p: String => Boolean): JsonTraversalPath =
    JsonTraversalPath(obj composeTraversal FilterIndex.filterIndex(p))

  final def as[A](implicit decode: Decoder[A], encode: Encoder[A]): Traversal[Json, A] =
    json.composePrism(
      Prism((j: Json) => decode.decodeJson(j).toOption)(encode(_))
    )
}
