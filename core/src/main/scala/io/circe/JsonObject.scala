package io.circe

import scala.collection.breakOut
import scala.collection.generic.IsTraversableOnce

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
   * Return all association keys in insertion order.
   */
  def fields: List[String]

  /**
   * Return all association keys in an undefined order.
   */
  def fieldSet: Set[String]

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
  def from[Repr](f: Repr)(implicit
    F: IsTraversableOnce[Repr] { type A = (String, Json) }
  ): JsonObject =
    F.conversion(f).foldLeft(empty) { case (acc, (k, v)) => acc + (k, v) }

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
   * Construct an empty [[JsonObject]].
   */
  val empty: JsonObject = MapAndVectorJsonObject(Map.empty, Vector.empty)

  /**
   * Construct a [[JsonObject]] with a single field.
   */
  def singleton(k: String, j: Json): JsonObject = MapAndVectorJsonObject(Map(k -> j), Vector(k))

  /**
   * A straightforward implementation of [[JsonObject]] with immutable collections.
   */
  private[circe] final case class MapAndVectorJsonObject(
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
    def fields: List[String] = orderedFields.toList
    def fieldSet: Set[String] = orderedFields.toSet

    def size: Int = fieldMap.size

    override def toString: String =
      "object[%s]".format(
        fieldMap.map {
          case (k, v) => s"$k -> $v"
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
