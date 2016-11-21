package io.circe

import cats.{ Applicative, Eq, Foldable, Show }
import cats.data.Kleisli
import cats.instances.map._
import scala.collection.breakOut

/**
 * A mapping from keys to JSON values that maintains insertion order.
 *
 * @author Tony Morris
 * @author Travis Brown
 */
sealed abstract class JsonObject extends Serializable {
  /**
   * Convert to a map.
   */
  def toMap: Map[String, Json]

  /**
   * Insert the given key-value pair.
   */
  def add(k: String, j: Json): JsonObject

  /**
   * Prepend the given key-value pair.
   */
  def +:(f: (String, Json)): JsonObject

  /**
   * Remove the field with the given key (if it exists).
   */
  def remove(k: String): JsonObject

  /**
   * Return the JSON value associated with the given field.
   */
  def apply(k: String): Option[Json]

  /**
   * Transform all associated JSON values.
   */
  def withJsons(f: Json => Json): JsonObject

  /**
   * Return `true` if there are no associations.
   */
  def isEmpty: Boolean

  /**
   * Return `true` if there is at least one association.
   */
  final def nonEmpty: Boolean = !isEmpty

  /**
   * Return `true` if there is an association with the given field.
   */
  def contains(f: String): Boolean

  /**
   * Return the list of associations in insertion order.
   */
  def toList: List[(String, Json)]

  /**
   * Return all associated values in insertion order.
   */
  def values: List[Json]

  /**
   * Return a Kleisli arrow that gets the JSON value associated with the given field.
   */
  def kleisli: Kleisli[Option, String, Json]

  /**
   * Return all association keys in insertion order.
   */
  def fields: List[String]

  /**
   * Return all association keys in an undefined order.
   */
  def fieldSet: Set[String]

  /**
   * Traverse [[Json]] values.
   */
  def traverse[F[_]](f: Json => F[Json])(implicit F: Applicative[F]): F[JsonObject]

  /**
   * Return the number of associations.
   */
  def size: Int

  /**
   * Filter by keys and values.
   */
  final def filter(pred: ((String, Json)) => Boolean): JsonObject = JsonObject.fromIterable(toList.filter(pred))

  /**
   * Filter by keys.
   */
  final def filterKeys(pred: String => Boolean): JsonObject = filter {
    case (k, _) => pred(k)
  }
}

/**
 * Constructors, type class instances, and other utilities for [[JsonObject]].
 */
final object JsonObject {
  /**
   * Construct a [[JsonObject]] from a foldable collection of key-value pairs.
   */
  final def from[F[_]](f: F[(String, Json)])(implicit F: Foldable[F]): JsonObject =
    F.foldLeft(f, empty) { case (acc, (k, v)) => acc.add(k, v) }

  /**
   * Construct a [[JsonObject]] from an [[scala.collection.Iterable]] (provided for optimization).
   */
  final def fromIterable(fields: Iterable[(String, Json)]): JsonObject = {
    val m = scala.collection.mutable.Map.empty[String, Json]
    val fs = Vector.newBuilder[String]

    val it = fields.iterator

    while (it.hasNext) {
      val (k, v) = it.next
      if (!m.contains(k)) fs += k else {}

      m(k) = v
    }

    MapAndVectorJsonObject(m.toMap, fs.result())
  }

  /**
   * Construct a [[JsonObject]] from a map from keys to [[Json]] values.
   *
   * Note that the order of the fields is arbitrary.
   */
  final def fromMap(m: Map[String, Json]): JsonObject = MapAndVectorJsonObject(m, m.keys.toVector)

  private[circe] final def fromMapAndVector(m: Map[String, Json], keys: Vector[String]): JsonObject =
    MapAndVectorJsonObject(m, keys)

  /**
   * Construct an empty [[JsonObject]].
   */
  final val empty: JsonObject = MapAndVectorJsonObject(Map.empty, Vector.empty)

  /**
   * Construct a [[JsonObject]] with a single field.
   */
  final def singleton(k: String, j: Json): JsonObject =
    MapAndVectorJsonObject(Map(k -> j), Vector(k))

  implicit final val showJsonObject: Show[JsonObject] = Show.fromToString
  implicit final val eqJsonObject: Eq[JsonObject] = Eq.by(_.toMap)

  /**
   * A straightforward implementation of [[JsonObject]] with immutable collections.
   */
  private[this] final case class MapAndVectorJsonObject(
    fieldMap: Map[String, Json],
    orderedFields: Vector[String]
  ) extends JsonObject {
    final def toMap: Map[String, Json] = fieldMap

    final def add(k: String, j: Json): JsonObject =
      if (fieldMap.contains(k)) {
        copy(fieldMap = fieldMap.updated(k, j))
      } else {
        copy(fieldMap = fieldMap.updated(k, j), orderedFields = orderedFields :+ k)
      }

    final def +:(f: (String, Json)): JsonObject = {
      val (k, j) = f
      if (fieldMap.contains(k)) {
        copy(fieldMap = fieldMap.updated(k, j))
      } else {
        copy(fieldMap = fieldMap.updated(k, j), orderedFields = k +: orderedFields)
      }
    }

    final def remove(k: String): JsonObject =
      copy(fieldMap = fieldMap - k, orderedFields = orderedFields.filterNot(_ == k))

    final def apply(k: String): Option[Json] = fieldMap.get(k)
    final def withJsons(f: Json => Json): JsonObject = copy(fieldMap = fieldMap.mapValues(f).view.force)
    final def isEmpty: Boolean = fieldMap.isEmpty
    final def contains(k: String): Boolean = fieldMap.contains(k)
    final def toList: List[(String, Json)] = orderedFields.map(k => k -> fieldMap(k))(breakOut)
    final def values: List[Json] = orderedFields.map(k => fieldMap(k))(breakOut)
    final def kleisli: Kleisli[Option, String, Json] = Kleisli(fieldMap.get)
    final def fields: List[String] = orderedFields.toList
    final def fieldSet: Set[String] = orderedFields.toSet

    final def traverse[F[_]](f: Json => F[Json])(implicit F: Applicative[F]): F[JsonObject] = F.map(
      orderedFields.foldLeft(F.pure(Map.empty[String, Json])) {
        case (acc, k) => F.map2(acc, f(fieldMap(k)))(_.updated(k, _))
      }
    )(mappedFields => copy(fieldMap = mappedFields))

    final def size: Int = fieldMap.size

    override final def toString: String =
      "object[%s]".format(
        fieldMap.map {
          case (k, v) => s"$k -> ${ Json.showJson.show(v) }"
        }.mkString(",")
      )

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
