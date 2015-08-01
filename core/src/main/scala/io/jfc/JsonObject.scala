package io.jfc

import algebra.Eq
import cats.{ Applicative, Foldable, Show }
import cats.data.Kleisli
import cats.std.map._

/**
 * A mapping from field to JSON value that maintains insertion order.
 *
 * @author Tony Morris
 */
sealed abstract class JsonObject {
  /**
   * Convert to a map.
   */
  def toMap: Map[String, Json]

  /**
   * Insert the given association.
   */
  def +(f: String, j: Json): JsonObject

  /**
   * Prepend the given association.
   */
  def +:(fj: (String, Json)): JsonObject

  /**
   * Remove the given field association.
   */
  def -(f: String): JsonObject

  /**
   * Return the JSON value associated with the given field.
   */
  def apply(f: String): Option[Json]

  /**
   * Transform all associated JSON values.
   */
  def withJsons(k: Json => Json): JsonObject

  /**
   * Returns true if there are no associations.
   */
  def isEmpty: Boolean

  /**
   * Returns true if there is at least one association.
   */
  def isNotEmpty: Boolean

  /**
   * Returns true if there is an association with the given field.
   */
  def ??(f: String): Boolean

  /**
   * Returns the list of associations in insertion order.
   */
  def toList: List[(String, Json)]

  /**
   * Returns all associated values in insertion order.
   */
  def values: List[Json]

  /**
   * Returns a kleisli function that gets the JSON value associated with the given field.
   */
  def kleisli: Kleisli[Option, String, Json]

  /**
   * Returns all association keys in insertion order.
   */
  def fields: List[String]

  /**
   * Returns all association keys in arbitrary order.
   */
  def fieldSet: Set[String]

  /**
   * Map Json values.
   */
  def map(f: Json => Json): JsonObject

  /**
   * Traverse Json values.
   */
  def traverse[F[_]](f: Json => F[Json])(implicit FF: Applicative[F]): F[JsonObject]

  /**
   * Returns the number of associations.
   */
  def size: Int
}

private[jfc] final case class JsonObjectInstance(
  fieldsMap: Map[String, Json] = Map.empty,
  orderedFields: Vector[String] = Vector.empty
) extends JsonObject {

  def toMap: Map[String, Json] = fieldsMap

  def +(f: String, j: Json): JsonObject =
    if (fieldsMap.contains(f)) {
      copy(fieldsMap = fieldsMap.updated(f, j))
    } else {
      copy(fieldsMap = fieldsMap.updated(f, j), orderedFields = orderedFields :+ f)
    }

  def +:(fj: (String, Json)): JsonObject = {
    val (f, j) = fj
    if (fieldsMap.contains(f))
      copy(fieldsMap = fieldsMap.updated(f, j))
    else
      copy(fieldsMap = fieldsMap.updated(f, j), orderedFields = f +: orderedFields)
  }

  def -(f: String): JsonObject =
    copy(fieldsMap = fieldsMap - f, orderedFields = orderedFields.filterNot(_ == f))

  def apply(f: String): Option[Json] = fieldsMap.get(f)

  def withJsons(k: Json => Json): JsonObject = map(k)

  def isEmpty: Boolean = fieldsMap.isEmpty

  def isNotEmpty: Boolean = !isEmpty

  def ??(f: String): Boolean = fieldsMap.contains(f)

  def toList: List[(String, Json)] = orderedFields.map(field => (field, fieldsMap(field))).toList

  def values: List[Json] = orderedFields.map(field => fieldsMap(field)).toList

  def kleisli: Kleisli[Option, String, Json] = Kleisli(fieldsMap get _)

  def fields: List[String] = orderedFields.toList

  def fieldSet: Set[String] = orderedFields.toSet

  def map(f: Json => Json): JsonObject = copy(fieldsMap = fieldsMap.foldLeft(Map.empty[String, Json]){case (acc, (key, value)) => acc.updated(key, f(value))})
  
  def traverse[F[_]](f: Json => F[Json])(implicit F: Applicative[F]): F[JsonObject] = F.map(
    orderedFields.foldLeft(F.pure(Map.empty[String, Json])) {
      case (acc, k) => F.ap(acc)(F.map(f(fieldsMap(k)))(j => _.updated(k, j)))
    }
  )(mappedFields => copy(fieldsMap = mappedFields))

  def size: Int = fields.size
  
  // TODO: clean up
  override def toString: String =
    "object[%s]".format(
      fieldsMap.map {
        case (k, v) => s"$k -> ${ Show[Json].show(v) }"
      }.mkString(",")
    )

  override def equals(o: Any) =
    o match {
      case JsonObjectInstance(otherMap, _) => fieldsMap == otherMap
      case _ => false
    }

  override def hashCode =
    fieldsMap.hashCode
}

object JsonObject extends JsonObjects {
  def from[F[_]](f: F[(String, Json)])(implicit F: Foldable[F]): JsonObject =
    F.foldLeft(f, empty) { case (acc, (k, v)) => acc + (k, v) }

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

    JsonObjectInstance(m.toMap, fs.result())
  }


  /**
   * Construct an empty association.
   */
  def empty: JsonObject = JsonObjectInstance()
}

trait JsonObjects {

  /**
   * Construct with a single association.
   */
  def single(f: String, j: Json): JsonObject =
    JsonObject.empty + (f, j)

  implicit val showJsonObject: Show[JsonObject] = Show.fromToString
  implicit val eqJsonObject: Eq[JsonObject] = Eq.by(_.toMap)
}
