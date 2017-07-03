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

    MapAndVectorJsonObject(map.toMap, keys.result())
  }

  /**
   * Construct a [[JsonObject]] from a map from keys to [[Json]] values.
   *
   * Note that the order of the fields is arbitrary.
   */
  final def fromMap(map: Map[String, Json]): JsonObject = MapAndVectorJsonObject(map, map.keys.toVector)

  private[circe] final def fromMapAndVector(map: Map[String, Json], keys: Vector[String]): JsonObject =
    MapAndVectorJsonObject(map, keys)

  /**
   * Construct an empty [[JsonObject]].
   */
  final val empty: JsonObject = MapAndVectorJsonObject(Map.empty, Vector.empty)

  /**
   * Construct a [[JsonObject]] with a single field.
   */
  final def singleton(key: String, value: Json): JsonObject =
    MapAndVectorJsonObject(Map((key, value)), Vector(key))

  implicit final val showJsonObject: Show[JsonObject] = Show.fromToString
  implicit final val eqJsonObject: Eq[JsonObject] = Eq.by(_.toMap)

  /**
   * A straightforward implementation of [[JsonObject]] with immutable collections.
   */
  private[this] final case class MapAndVectorJsonObject(
    fieldMap: Map[String, Json],
    orderedFields: Vector[String]
  ) extends JsonObject {
    final def apply(key: String): Option[Json] = fieldMap.get(key)
    final def size: Int = fieldMap.size
    final def contains(key: String): Boolean = fieldMap.contains(key)
    final def isEmpty: Boolean = fieldMap.isEmpty

    final def keys: Iterable[String] = orderedFields
    final def values: Iterable[Json] = orderedFields.toIterable.map(key => fieldMap(key))(breakOut)

    final def toMap: Map[String, Json] = fieldMap
    final def toIterable: Iterable[(String, Json)] = orderedFields.toIterable.map(key => (key, fieldMap(key)))

    final def add(key: String, value: Json): JsonObject =
      if (fieldMap.contains(key)) {
        copy(fieldMap = fieldMap.updated(key, value))
      } else {
        copy(fieldMap = fieldMap.updated(key, value), orderedFields = orderedFields :+ key)
      }

    final def +:(field: (String, Json)): JsonObject = {
      val (key, value) = field
      if (fieldMap.contains(key)) {
        copy(fieldMap = fieldMap.updated(key, value))
      } else {
        copy(fieldMap = fieldMap.updated(key, value), orderedFields = key +: orderedFields)
      }
    }

    final def remove(key: String): JsonObject =
      copy(fieldMap = fieldMap - key, orderedFields = orderedFields.filterNot(_ == key))

    final def traverse[F[_]](f: Json => F[Json])(implicit F: Applicative[F]): F[JsonObject] = F.map(
      orderedFields.foldLeft(F.pure(Map.empty[String, Json])) {
        case (acc, k) => F.map2(acc, f(fieldMap(k)))(_.updated(k, _))
      }
    )(mappedFields => copy(fieldMap = mappedFields))

    final def mapValues(f: Json => Json): JsonObject = copy(fieldMap = fieldMap.mapValues(f).view.force)

    override final def toString: String =
      fieldMap.map {
        case (k, v) => s"$k -> ${ Json.showJson.show(v) }"
      }.mkString("object[", ",", "]")

    /**
     * Universal equality derived from our type-safe equality.
     */
    override final def equals(that: Any): Boolean = that match {
      case that: JsonObject => JsonObject.eqJsonObject.eqv(this, that)
      case _ => false
    }

    override final def hashCode: Int = fieldMap.hashCode
  }
}
