package io.circe

import cats.{ Applicative, Eq, Foldable, Show }
import cats.data.Kleisli
import java.io.Serializable
import java.util.LinkedHashMap
import scala.collection.immutable.{ Map, Set }

/**
 * A mapping from keys to JSON values that maintains insertion order.
 *
 * @author Travis Brown
 *
 * @groupname Contents Operations for accessing contents
 * @groupprio Contents 0
 *
 * @groupname Conversions Conversions to other collection types
 * @groupprio Conversions 1
 *
 * @groupname Modification Operations that transform the JSON object
 * @groupprio Modification 2
 *
 * @groupname Other Equality and other operations
 * @groupprio Other 3
 */
sealed abstract class JsonObject extends Serializable {
  /**
   * Return the JSON value associated with the given key, with undefined behavior if there is none.
   *
   * @group Contents
   */
  private[circe] def applyUnsafe(k: String): Json

  /**
   * Return the JSON value associated with the given key.
   *
   * @group Contents
   */
  def apply(key: String): Option[Json]

  /**
   * Return `true` if there is an association with the given key.
   *
   * @group Contents
   */
  def contains(key: String): Boolean

  /**
   * Return the number of associations.
   *
   * @group Contents
   */
  def size: Int

  /**
   * Return `true` if there are no associations.
   *
   * @group Contents
   */
  def isEmpty: Boolean

  /**
   * Return `true` if there is at least one association.
   *
   * @group Contents
   */
  final def nonEmpty: Boolean = !isEmpty

  /**
   * Return a Kleisli arrow that gets the JSON value associated with the given field.
   *
   * @group Contents
   */
  final val kleisli: Kleisli[Option, String, Json] = Kleisli(apply(_))

  /**
   * Return all keys in insertion order.
   *
   * @group Contents
   */
  def keys: Iterable[String]

  /**
   * Return all associated values in insertion order.
   *
   * @group Contents
   */
  def values: Iterable[Json]

  /**
   * Return all keys in insertion order.
   *
   * @group Contents
   */
  @deprecated("Use keys", "0.9.0")
  final def fields: Iterable[String] = keys

  /**
   * Return all keys in an undefined order.
   *
   * @group Contents
   */
  @deprecated("Use key.toSet", "0.9.0")
  final def fieldSet: Set[String] = keys.toSet

  /**
   * Convert to a map.
   *
   * @note This conversion does not maintain insertion order.
   * @group Conversions
   */
  def toMap: Map[String, Json]

  /**
   * Return all key-value pairs in insertion order.
   *
   * @group Conversions
   */
  def toIterable: Iterable[(String, Json)]

  /**
   * Return all key-value pairs in insertion order as a list.
   *
   * @group Conversions
   */
  final def toList: List[(String, Json)] = toIterable.toList

  /**
   * Return all key-value pairs in insertion order as a vector.
   *
   * @group Conversions
   */
  final def toVector: Vector[(String, Json)] = toIterable.toVector

  /**
   * Insert the given key and value.
   *
   * @group Modification
   */
  def add(key: String, value: Json): JsonObject

  /**
   * Prepend the given key-value pair.
   *
   * @group Modification
   */
  def +:(field: (String, Json)): JsonObject

  /**
   * Remove the field with the given key (if it exists).
   *
   * @group Modification
   */
  def remove(key: String): JsonObject

  /**
   * Traverse [[Json]] values.
   *
   * @group Modification
   */
  def traverse[F[_]](f: Json => F[Json])(implicit F: Applicative[F]): F[JsonObject]

  /**
   * Transform all associated JSON values.
   *
   * @group Modification
   */
  def mapValues(f: Json => Json): JsonObject

  /**
   * Transform all associated JSON values.
   *
   * @group Modification
   */
  @deprecated("Use mapValues", "0.9.0")
  final def withJsons(f: Json => Json): JsonObject = mapValues(f)

  /**
   * Filter by keys and values.
   *
   * @group Modification
   */
  final def filter(pred: ((String, Json)) => Boolean): JsonObject = JsonObject.fromIterable(toIterable.filter(pred))

  /**
   * Filter by keys.
   *
   * @group Modification
   */
  final def filterKeys(pred: String => Boolean): JsonObject = filter(field => pred(field._1))

  private[circe] def appendToFolder(folder: Printer.PrintingFolder): Unit

  /**
   * @group Other
   */
  final override def toString: String = toIterable.map {
    case (k, v) => s"$k -> ${ Json.showJson.show(v) }"
  }.mkString("object[", ",", "]")

  /**
   * @group Other
   */
  final override def equals(that: Any): Boolean = if (that.isInstanceOf[JsonObject]) {
    toMap == that.asInstanceOf[JsonObject].toMap
  } else {
    false
  }

  /**
   * @group Other
   */
  final override def hashCode: Int = toMap.hashCode
}

/**
 * Constructors, type class instances, and other utilities for [[JsonObject]].
 */
final object JsonObject {
  /**
   * Construct a [[JsonObject]] from the given key-value pairs.
   */
  final def apply(fields: (String, Json)*): JsonObject = fromIterable(fields)

  /**
   * Construct a [[JsonObject]] from a foldable collection of key-value pairs.
   */
  final def fromFoldable[F[_]](fields: F[(String, Json)])(implicit F: Foldable[F]): JsonObject =
    F.foldLeft(fields, empty) { case (acc, (key, value)) => acc.add(key, value) }

  /**
   * Construct a [[JsonObject]] from a foldable collection of key-value pairs.
   */
  @deprecated("Use fromFoldable", "0.9.0")
  final def from[F[_]](fields: F[(String, Json)])(implicit F: Foldable[F]): JsonObject = fromFoldable[F](fields)(F)

  /**
   * Construct a [[JsonObject]] from an [[scala.collection.Iterable]] (provided for optimization).
   */
  final def fromIterable(fields: Iterable[(String, Json)]): JsonObject = {
    val map = new LinkedHashMap[String, Json]
    val iterator = fields.iterator

    while (iterator.hasNext) {
      val (key, value) = iterator.next

      map.put(key, value)
    }

    new LinkedHashMapJsonObject(map)
  }

  /**
   * Construct a [[JsonObject]] from a map from keys to [[Json]] values.
   *
   * Note that the order of the fields is arbitrary.
   */
  final def fromMap(map: Map[String, Json]): JsonObject = new MapAndVectorJsonObject(map, map.keys.toVector)

  private[circe] final def fromLinkedHashMap(map: LinkedHashMap[String, Json]): JsonObject =
    new LinkedHashMapJsonObject(map)

  /**
   * Construct an empty [[JsonObject]].
   */
  final val empty: JsonObject = new MapAndVectorJsonObject(Map.empty, Vector.empty)

  /**
   * Construct a [[JsonObject]] with a single field.
   */
  final def singleton(key: String, value: Json): JsonObject = new MapAndVectorJsonObject(Map((key, value)), Vector(key))

  implicit final val showJsonObject: Show[JsonObject] = Show.fromToString
  implicit final val eqJsonObject: Eq[JsonObject] = Eq.fromUniversalEquals

  /**
   * An implementation of [[JsonObject]] built on `java.util.LinkedHashMap`.
   */
  private[this] final class LinkedHashMapJsonObject(fields: LinkedHashMap[String, Json]) extends JsonObject {
    private[circe] def applyUnsafe(key: String): Json = fields.get(key)
    final def apply(k: String): Option[Json] = Option(fields.get(k))
    final def size: Int = fields.size
    final def contains(k: String): Boolean = fields.containsKey(k)
    final def isEmpty: Boolean = fields.isEmpty

    final def keys: Iterable[String] = new Iterable[String] {
      final def iterator: Iterator[String] = new Iterator[String] {
        private[this] val underlying = fields.keySet.iterator

        final def hasNext: Boolean = underlying.hasNext
        final def next(): String = underlying.next()
      }
    }

    final def values: Iterable[Json] = new Iterable[Json] {
      final def iterator: Iterator[Json] = new Iterator[Json] {
        private[this] val underlying = fields.values.iterator

        final def hasNext: Boolean = underlying.hasNext
        final def next(): Json = underlying.next()
      }
    }

    final def toMap: Map[String, Json] = {
      val builder = Map.newBuilder[String, Json]
      builder.sizeHint(size)

      val iterator = fields.entrySet.iterator

      while (iterator.hasNext) {
        val next = iterator.next

        builder += ((next.getKey, next.getValue))
      }

      builder.result()
    }

    final def toIterable: Iterable[(String, Json)] = new Iterable[(String, Json)] {
      final def iterator: Iterator[(String, Json)] = new Iterator[(String, Json)] {
        private[this] val underlying = fields.entrySet.iterator

        final def hasNext: Boolean = underlying.hasNext
        final def next(): (String, Json) = {
          val field = underlying.next()

          (field.getKey, field.getValue)
        }
      }
    }

    final def appendToFolder(folder: Printer.PrintingFolder): Unit = {
      val originalDepth = folder.depth
      val p = folder.pieces(folder.depth)
      var first = true
      val iterator = fields.entrySet.iterator

      folder.writer.append(p.lBraces)

      while (iterator.hasNext) {
        val next = iterator.next()
        val key = next.getKey
        val value = next.getValue

        if (!folder.dropNullValues || !value.isNull) {
          if (!first) folder.writer.append(p.objectCommas)
          folder.onString(key)
          folder.writer.append(p.colons)

          folder.depth += 1
          value.foldWith(folder)
          folder.depth = originalDepth
          first = false
        }
      }

      folder.writer.append(p.rBraces)
    }

    private[this] def toMapAndVectorJsonObject: MapAndVectorJsonObject = {
      val mapBuilder = Map.newBuilder[String, Json]
      val keyBuilder = Vector.newBuilder[String]
      mapBuilder.sizeHint(size)
      keyBuilder.sizeHint(size)

      val iterator = fields.entrySet.iterator

      while (iterator.hasNext) {
        val next = iterator.next
        val key = next.getKey

        mapBuilder += ((key, next.getValue))
        keyBuilder += key
      }

      new MapAndVectorJsonObject(mapBuilder.result(), keyBuilder.result())
    }

    final def add(k: String, j: Json): JsonObject = toMapAndVectorJsonObject.add(k, j)
    final def +:(f: (String, Json)): JsonObject = toMapAndVectorJsonObject.+:(f)
    final def remove(k: String): JsonObject = toMapAndVectorJsonObject.remove(k)

    final def traverse[F[_]](f: Json => F[Json])(implicit F: Applicative[F]): F[JsonObject] =
      toMapAndVectorJsonObject.traverse[F](f)(F)

    final def mapValues(f: Json => Json): JsonObject = toMapAndVectorJsonObject.mapValues(f)
  }

  /**
   * A straightforward implementation of [[JsonObject]] with immutable collections.
   */
  private[this] final class MapAndVectorJsonObject(
    fields: Map[String, Json],
    orderedKeys: Vector[String]
  ) extends JsonObject {
    private[circe] def applyUnsafe(key: String): Json = fields(key)
    final def apply(key: String): Option[Json] = fields.get(key)
    final def size: Int = fields.size
    final def contains(key: String): Boolean = fields.contains(key)
    final def isEmpty: Boolean = fields.isEmpty

    final def keys: Iterable[String] = orderedKeys
    final def values: Iterable[Json] = orderedKeys.map(key => fields(key))

    final def toMap: Map[String, Json] = fields
    final def toIterable: Iterable[(String, Json)] = orderedKeys.map(key => (key, fields(key)))

    final def add(key: String, value: Json): JsonObject =
      if (fields.contains(key)) {
        new MapAndVectorJsonObject(fields.updated(key, value), orderedKeys)
      } else {
        new MapAndVectorJsonObject(fields.updated(key, value), orderedKeys :+ key)
      }

    final def +:(field: (String, Json)): JsonObject = {
      val (key, value) = field
      if (fields.contains(key)) {
        new MapAndVectorJsonObject(fields.updated(key, value), orderedKeys)
      } else {
        new MapAndVectorJsonObject(fields.updated(key, value), key +: orderedKeys)
      }
    }

    final def remove(key: String): JsonObject =
      new MapAndVectorJsonObject(fields - key, orderedKeys.filterNot(_ == key))

    final def traverse[F[_]](f: Json => F[Json])(implicit F: Applicative[F]): F[JsonObject] = F.map(
      orderedKeys.foldLeft(F.pure(Map.empty[String, Json])) {
        case (acc, key) => F.map2(acc, f(fields(key)))(_.updated(key, _))
      }
    )(mappedFields => new MapAndVectorJsonObject(mappedFields, orderedKeys))

    final def mapValues(f: Json => Json): JsonObject =
      new MapAndVectorJsonObject(fields.mapValues(f).view.force, orderedKeys)

    final def appendToFolder(folder: Printer.PrintingFolder): Unit = {
      val originalDepth = folder.depth
      val p = folder.pieces(folder.depth)
      var first = true
      val keyIterator = orderedKeys.iterator

      folder.writer.append(p.lBraces)

      while (keyIterator.hasNext) {
        val key = keyIterator.next()
        val value = fields(key)
        if (!folder.dropNullValues || !value.isNull) {
          if (!first) folder.writer.append(p.objectCommas)
          folder.onString(key)
          folder.writer.append(p.colons)

          folder.depth += 1
          value.foldWith(folder)
          folder.depth = originalDepth
          first = false
        }
      }

      folder.writer.append(p.rBraces)
    }
  }
}
