package io.circe.optics

import io.circe.{ Decoder, Encoder, Json, JsonNumber, JsonObject }
import io.circe.optics.JsonObjectOptics._
import io.circe.optics.JsonOptics._
import monocle.{ Fold, Optional, Prism, Traversal }
import monocle.function.{ At, FilterIndex, Index }
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
  final def arr: Optional[Json, Vector[Json]] = json composePrism jsonArray
  final def obj: Optional[Json, JsonObject] = json composePrism jsonObject

  final def at(field: String): Optional[Json, Option[Json]] =
    json.composePrism(jsonObject).composeLens(At.at(field))

  final def selectDynamic(field: String): JsonPath =
    JsonPath(json.composePrism(jsonObject).composeOptional(Index.index(field)))

  final def applyDynamic(field: String)(index: Int): JsonPath = selectDynamic(field).index(index)

  final def apply(i: Int): JsonPath = index(i)

  final def index(i: Int): JsonPath =
    JsonPath(json.composePrism(jsonArray).composeOptional(Index.index(i)))

  final def each: JsonTraversalPath =
    JsonTraversalPath(json composeTraversal jsonDescendants)

  final def filterByIndex(p: Int => Boolean): JsonTraversalPath =
    JsonTraversalPath(arr composeTraversal FilterIndex.filterIndex(p))

  final def filterByField(p: String => Boolean): JsonTraversalPath =
    JsonTraversalPath(obj composeTraversal FilterIndex.filterIndex(p))

  final def filterUnsafe(p: Json => Boolean): JsonPath =
    JsonPath(json composePrism UnsafeOptics.select(p))

  final def filter(p: Json => Boolean): JsonFoldPath =
    JsonFoldPath(filterUnsafe(p).json.asFold)

  final def as[A](implicit decode: Decoder[A], encode: Encoder[A]): Optional[Json, A] =
    json composePrism UnsafeOptics.parse
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
  final def arr: Traversal[Json, Vector[Json]] = json composePrism jsonArray
  final def obj: Traversal[Json, JsonObject] = json composePrism jsonObject

  final def at(field: String): Traversal[Json, Option[Json]] =
    json.composePrism(jsonObject).composeLens(At.at(field))

  final def selectDynamic(field: String): JsonTraversalPath =
    JsonTraversalPath(json.composePrism(jsonObject).composeOptional(Index.index(field)))

  final def applyDynamic(field: String)(index: Int): JsonTraversalPath = selectDynamic(field).index(index)

  final def apply(i: Int): JsonTraversalPath = index(i)

  final def index(i: Int): JsonTraversalPath =
    JsonTraversalPath(json.composePrism(jsonArray).composeOptional(Index.index(i)))

  final def each: JsonTraversalPath =
    JsonTraversalPath(json composeTraversal jsonDescendants)

  final def filterByIndex(p: Int => Boolean): JsonTraversalPath =
    JsonTraversalPath(arr composeTraversal FilterIndex.filterIndex(p))

  final def filterByField(p: String => Boolean): JsonTraversalPath =
    JsonTraversalPath(obj composeTraversal FilterIndex.filterIndex(p))

  final def filterUnsafe(p: Json => Boolean): JsonTraversalPath =
    JsonTraversalPath(json composePrism UnsafeOptics.select(p))

  final def filter(p: Json => Boolean): JsonFoldPath =
    JsonFoldPath(filterUnsafe(p).json.asFold)

  final def as[A](implicit decode: Decoder[A], encode: Encoder[A]): Traversal[Json, A] =
    json composePrism UnsafeOptics.parse
}

final case class JsonFoldPath(json: Fold[Json, Json]) extends Dynamic {
  final def `null`: Fold[Json, Unit] = json composePrism jsonNull
  final def boolean: Fold[Json, Boolean] = json composePrism jsonBoolean
  final def byte: Fold[Json, Byte] = json composePrism jsonByte
  final def short: Fold[Json, Short] = json composePrism jsonShort
  final def int: Fold[Json, Int] = json composePrism jsonInt
  final def long: Fold[Json, Long] = json composePrism jsonLong
  final def bigInt: Fold[Json, BigInt] = json composePrism jsonBigInt
  final def double: Fold[Json, Double] = json composePrism jsonDouble
  final def bigDecimal: Fold[Json, BigDecimal] = json composePrism jsonBigDecimal
  final def number: Fold[Json, JsonNumber] = json composePrism jsonNumber
  final def string: Fold[Json, String] = json composePrism jsonString
  final def arr: Fold[Json, Vector[Json]] = json composePrism jsonArray
  final def obj: Fold[Json, JsonObject] = json composePrism jsonObject

  final def at(field: String): Fold[Json, Option[Json]] =
    json.composePrism(jsonObject).composeLens(At.at(field))

  final def selectDynamic(field: String): JsonFoldPath =
    JsonFoldPath(json.composePrism(jsonObject).composeOptional(Index.index(field)))

  final def applyDynamic(field: String)(index: Int): JsonFoldPath = selectDynamic(field).index(index)

  final def apply(i: Int): JsonFoldPath = index(i)

  final def index(i: Int): JsonFoldPath =
    JsonFoldPath(json.composePrism(jsonArray).composeOptional(Index.index(i)))

  final def each: JsonFoldPath =
    JsonFoldPath(json composeTraversal jsonDescendants)

  final def filterByIndex(p: Int => Boolean): JsonFoldPath =
    JsonFoldPath(arr composeTraversal FilterIndex.filterIndex(p))

  final def filterByField(p: String => Boolean): JsonFoldPath =
    JsonFoldPath(obj composeTraversal FilterIndex.filterIndex(p))

  final def filter(p: Json => Boolean): JsonFoldPath =
    JsonFoldPath(json composePrism UnsafeOptics.select(p))

  final def as[A](implicit decode: Decoder[A], encode: Encoder[A]): Fold[Json, A] =
    json composePrism UnsafeOptics.parse
}

object UnsafeOptics {
  /**
    * Decode a value at the current location.
    *
    * Note that this operation is not lawful, since decoding is not injective (as noted by Julien
    * Truffaut). It is provided here for convenience, but may change in future versions.
    */
  def parse[A](implicit decode: Decoder[A], encode: Encoder[A]): Prism[Json, A] =
    Prism[Json, A](decode.decodeJson(_) match {
      case Right(a) => Some(a)
      case Left(_) => None
    })(encode(_))

  /**
    * Select if a value matches a predicate
    *
    * Note that this operation is not lawful because the predicate could be invalidated with set or modify.
    * However `select(_.a > 10) composeLens b` is safe because once we zoom into `b`, we cannot change `a` anymore.
    */
  def select[A](p: A => Boolean): Prism[A, A] =
    Prism[A, A](a => if (p(a)) Some(a) else None)(identity)
}
