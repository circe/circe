package io.circe

import algebra.Eq
import cats.{ Applicative, Foldable, Show }
import cats.data.Kleisli
import cats.std.map._
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
   * Insert the given association.
   */
  def +(k: String, j: Json): JsonObject

  /**
   * Prepend the given association.
   */
  def +:(f: (String, Json)): JsonObject

  /**
   * Remove the given field association.
   */
  def -(k: String): JsonObject

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
  def nonEmpty: Boolean = !isEmpty

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
   * Type-safe equality for [[JsonObject]].
   */
  def ===(that: JsonObject): Boolean = this.toMap == that.toMap

  /**
   * Type-safe inequality for [[JsonObject]].
   */
  def =!=(that: JsonObject): Boolean = !(this === that)
}

/**
 * Constructors, type class instances, and other utilities for [[JsonObject]].
 */
object JsonObject {
  /**
   * Construct a [[JsonObject]] from a foldable collection of key-value pairs.
   */
  def from[F[_]](f: F[(String, Json)])(implicit F: Foldable[F]): JsonObject =
    F.foldLeft(f, empty) { case (acc, (k, v)) => acc + (k, v) }

  /**
   * Construct a [[JsonObject]] from an [[scala.collection.IndexedSeq]] (provided for optimization).
   */
  def fromIndexedSeq(f: IndexedSeq[(String, Json)]): JsonObject = {
    var i = 0
    val m = scala.collection.mutable.Map.empty[String, Json]
    val fs = Vector.newBuilder[String]

    while (i < f.size) {
      val item = f(i)
      if (!m.contains(item._1)) {
        fs += item._1
      }
      m(item._1) = item._2
      i += 1
    }

    MapAndVectorJsonObject(m.toMap, fs.result())
  }

  /**
   * Construct a [[JsonObject]] from a map from keys to [[Json]] values.
   *
   * Note that the order of the fields is arbitrary.
   */
  def fromMap(m: Map[String, Json]): JsonObject = MapAndVectorJsonObject(m, m.keys.toVector)

  /**
   * Construct an empty [[JsonObject]].
   */
  val empty: JsonObject = MapAndVectorJsonObject(Map.empty, Vector.empty)

  /**
   * Construct a [[JsonObject]] with a single field.
   */
  def singleton(k: String, j: Json): JsonObject = MapAndVectorJsonObject(Map(k -> j), Vector(k))

  implicit val showJsonObject: Show[JsonObject] = Show.fromToString
  implicit val eqJsonObject: Eq[JsonObject] = Eq.by(_.toMap)

  /**
   * A straightforward implementation of [[JsonObject]] with immutable collections.
   */
  private[this] final case class MapAndVectorJsonObject(
    fieldMap: Map[String, Json],
    orderedFields: Vector[String]
  ) extends JsonObject {
    def toMap: Map[String, Json] = fieldMap

    def +(k: String, j: Json): JsonObject =
      if (fieldMap.contains(k)) {
        copy(fieldMap = fieldMap.updated(k, j))
      } else {
        copy(fieldMap = fieldMap.updated(k, j), orderedFields = orderedFields :+ k)
      }

    def +:(f: (String, Json)): JsonObject = {
      val (k, j) = f
      if (fieldMap.contains(k)) {
        copy(fieldMap = fieldMap.updated(k, j))
      } else {
        copy(fieldMap = fieldMap.updated(k, j), orderedFields = k +: orderedFields)
      }
    }

    def -(k: String): JsonObject =
      copy(fieldMap = fieldMap - k, orderedFields = orderedFields.filterNot(_ == k))

    def apply(k: String): Option[Json] = fieldMap.get(k)
    def withJsons(f: Json => Json): JsonObject = copy(fieldMap = fieldMap.mapValues(f).view.force)
    def isEmpty: Boolean = fieldMap.isEmpty
    def contains(k: String): Boolean = fieldMap.contains(k)
    def toList: List[(String, Json)] = orderedFields.map(k => k -> fieldMap(k))(breakOut)
    def values: List[Json] = orderedFields.map(k => fieldMap(k))(breakOut)
    def kleisli: Kleisli[Option, String, Json] = Kleisli(fieldMap.get)
    def fields: List[String] = orderedFields.toList
    def fieldSet: Set[String] = orderedFields.toSet

    def traverse[F[_]](f: Json => F[Json])(implicit F: Applicative[F]): F[JsonObject] = F.map(
      orderedFields.foldLeft(F.pure(Map.empty[String, Json])) {
        case (acc, k) => F.ap(acc)(F.map(f(fieldMap(k)))(j => _.updated(k, j)))
      }
    )(mappedFields => copy(fieldMap = mappedFields))

    def size: Int = fieldMap.size

    override def toString: String =
      "object[%s]".format(
        fieldMap.map {
          case (k, v) => s"$k -> ${ Json.showJson.show(v) }"
        }.mkString(",")
      )

    /**
     * Universal equality derived from our type-safe equality.
     */
    override def equals(o: Any) =
      o match {
        case j: JsonObject => this === j
        case _ => false
      }

    override def hashCode = fieldMap.hashCode
  }
}
