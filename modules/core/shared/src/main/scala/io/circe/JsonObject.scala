package io.circe

import cats.{ Applicative, Eq, Foldable, Show }
import cats.data.Kleisli
import cats.instances.map._
import java.io.Serializable
import scala.collection.breakOut
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
 * @groupprio Modification
 */
sealed abstract class JsonObject extends Serializable {
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
  final def from[F[_]](fields: F[(String, Json)])(implicit F: Foldable[F]): JsonObject =
    F.foldLeft(fields, empty) { case (acc, (key, value)) => acc.add(key, value) }

  /**
   * Construct a [[JsonObject]] from an [[scala.collection.Iterable]] (provided for optimization).
   */
  final def fromIterable(fields: Iterable[(String, Json)]): JsonObject = {
    val map = scala.collection.mutable.Map.empty[String, Json]
    val keys = Vector.newBuilder[String]

    val iterator = fields.iterator

    while (iterator.hasNext) {
      val (key, value) = iterator.next
      if (!map.contains(key)) keys += key else {}

      map(key) = value
    }

    new MapAndVectorJsonObject(map.toMap, keys.result())
  }

  /**
   * Construct a [[JsonObject]] from a map from keys to [[Json]] values.
   *
   * Note that the order of the fields is arbitrary.
   */
  final def fromMap(map: Map[String, Json]): JsonObject = new MapAndVectorJsonObject(map, map.keys.toVector)

  private[circe] final def fromMapAndVector(map: Map[String, Json], keys: Vector[String]): JsonObject =
    new MapAndVectorJsonObject(map, keys)

  /**
   * Construct an empty [[JsonObject]].
   */
  final val empty: JsonObject = new MapAndVectorJsonObject(Map.empty, Vector.empty)

  /**
   * Construct a [[JsonObject]] with a single field.
   */
  final def singleton(key: String, value: Json): JsonObject =
    new MapAndVectorJsonObject(Map((key, value)), Vector(key))

  implicit final val showJsonObject: Show[JsonObject] = Show.fromToString
  implicit final val eqJsonObject: Eq[JsonObject] = Eq.by(_.toMap)

  /**
   * A straightforward implementation of [[JsonObject]] with immutable collections.
   */
  private[this] final class MapAndVectorJsonObject(
    fields: Map[String, Json],
    orderedKeys: Vector[String]
  ) extends JsonObject {
    final def apply(key: String): Option[Json] = fields.get(key)
    final def size: Int = fields.size
    final def contains(key: String): Boolean = fields.contains(key)
    final def isEmpty: Boolean = fields.isEmpty

    final def keys: Iterable[String] = orderedKeys
    final def values: Iterable[Json] = orderedKeys.toIterable.map(key => fields(key))(breakOut)

    final def toMap: Map[String, Json] = fields
    final def toIterable: Iterable[(String, Json)] = orderedKeys.toIterable.map(key => (key, fields(key)))

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
        case (acc, k) => F.map2(acc, f(fields(k)))(_.updated(k, _))
      }
    )(mappedFields => new MapAndVectorJsonObject(mappedFields, orderedKeys))

    final def mapValues(f: Json => Json): JsonObject =
      new MapAndVectorJsonObject(fields.mapValues(f).view.force, orderedKeys)

    override final def toString: String =
      fields.map {
        case (k, v) => s"$k -> ${ Json.showJson.show(v) }"
      }.mkString("object[", ",", "]")

    final def appendToFolder(folder: Printer.PrintingFolder): Unit = {
      val originalDepth = folder.depth
      val p = folder.pieces(folder.depth)
      var first = true
      val keyIterator = orderedKeys.iterator

      folder.writer.append(p.lBraces)

      while (keyIterator.hasNext) {
        val key = keyIterator.next()
        val value = fields(key)
        if (!folder.dropNullKeys || !value.isNull) {
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

    /**
     * Universal equality derived from our type-safe equality.
     */
    override final def equals(that: Any): Boolean = that match {
      case that: JsonObject => JsonObject.eqJsonObject.eqv(this, that)
      case _ => false
    }

    override final def hashCode: Int = fields.hashCode
  }
}
