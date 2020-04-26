package io.circe

import cats.{ Applicative, Eq, Foldable, Show }
import cats.data.Kleisli
import java.io.Serializable
import java.util.LinkedHashMap
import scala.collection.immutable.Map
import scala.Predef.{<:<}

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
sealed abstract class JsonObject[J] extends Serializable {

  /**
   * Return the JSON value associated with the given key, with undefined behavior if there is none.
   *
   * @group Contents
   */
  private[circe] def applyUnsafe(k: String): J

  /**
   * Return the JSON value associated with the given key.
   *
   * @group Contents
   */
  def apply(key: String): Option[J]

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
  final def kleisli: Kleisli[Option, String, J] = Kleisli(apply(_))

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
  def values: Iterable[J]

  /**
   * Convert to a map.
   *
   * @note This conversion does not maintain insertion order.
   * @group Conversions
   */
  def toMap: Map[String, J]

  /**
   * Return all key-value pairs in insertion order.
   *
   * @group Conversions
   */
  def toIterable: Iterable[(String, J)]

  /**
   * Return all key-value pairs in insertion order as a list.
   *
   * @group Conversions
   */
  final def toList: List[(String, J)] = toIterable.toList

  /**
   * Return all key-value pairs in insertion order as a vector.
   *
   * @group Conversions
   */
  final def toVector: Vector[(String, J)] = toIterable.toVector

  /**
   * Insert the given key and value.
   *
   * @group Modification
   */
  def add(key: String, value: J): JsonObject[J]

  /**
   * Prepend the given key-value pair.
   *
   * @group Modification
   */
  def +:(field: (String, J)): JsonObject[J]

  /**
   * Remove the field with the given key (if it exists).
   *
   * @group Modification
   */
  def remove(key: String): JsonObject[J]

  /**
   * Traverse [[Json]] values.
   *
   * @group Modification
   */
  def traverse[F[_], K](f: J => F[K])(implicit F: Applicative[F]): F[JsonObject[K]]

  /**
   * Transform all associated JSON values.
   *
   * @group Modification
   */
  def mapValues[K](f: J => K): JsonObject[K]

  /**
   * Filter by keys and values.
   *
   * @group Modification
   */
  final def filter(pred: ((String, J)) => Boolean): JsonObject[J] = JsonObject.fromIterable(toIterable.filter(pred))

  /**
   * Filter by keys.
   *
   * @group Modification
   */
  final def filterKeys(pred: String => Boolean): JsonObject[J] = filter(field => pred(field._1))

  /**
   * Perform a deep merge of this JSON object with another JSON object.
   *
   * Objects are merged by key, values from the argument JSON take
   * precedence over values from this JSON. Nested objects are
   * recursed.
   *
   * See [[Json.deepMerge]] for behavior of merging values that are not objects.
   */
  def deepMerge(that: JsonObject[J], dm: (J, J) => J): JsonObject[J] =
    toIterable.foldLeft(that) {
      case (acc, (key, value)) =>
        that(key).fold(acc.add(key, value)) { r =>
          acc.add(key, dm(value, r))
        }
    }

  private[circe] def appendToFolder(folder: Printer.PrintingFolder)(implicit ev: J <:< Json): Unit

  /**
   * @group Other
   */
  final override def equals(that: Any): Boolean = that match {
    case that: JsonObject[_] => toMap == that.toMap
    case _                   => false
  }

  /**
   * @group Other
   */
  final override def hashCode: Int = toMap.hashCode
}

/**
 * Constructors, type class instances, and other utilities for [[JsonObject]].
 */
object JsonObject {

  /**
   * Construct a [[JsonObject]] from the given key-value pairs.
   */
  final def apply[J](fields: (String, J)*): JsonObject[J] = fromIterable(fields)

  /**
   * Construct a [[JsonObject]] from a foldable collection of key-value pairs.
   */
  final def fromFoldable[F[_], J](fields: F[(String, J)])(implicit F: Foldable[F]): JsonObject[J] =
    F.foldLeft(fields, empty[J]) { case (acc, (key, value)) => acc.add(key, value) }

  /**
   * Construct a [[JsonObject]] from an [[scala.collection.Iterable]] (provided for optimization).
   */
  final def fromIterable[J](fields: Iterable[(String, J)]): JsonObject[J] = {
    val map = new LinkedHashMap[String, J]
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
  final def fromMap[J](map: Map[String, J]): JsonObject[J] = fromMapAndVector(map, map.keys.toVector)

  private[circe] final def fromMapAndVector[J](map: Map[String, J], keys: Vector[String]): JsonObject[J] =
    new MapAndVectorJsonObject(map, keys)

  private[circe] final def fromLinkedHashMap[J](map: LinkedHashMap[String, J]): JsonObject[J] =
    new LinkedHashMapJsonObject(map)

  /**
   * Construct an empty [[JsonObject]].
   */
  final def empty[J]: JsonObject[J] = new MapAndVectorJsonObject(Map.empty, Vector.empty)

  /**
   * Construct a [[JsonObject]] with a single field.
   */
  final def singleton[J](key: String, value: J): JsonObject[J] = new MapAndVectorJsonObject(Map((key, value)), Vector(key))

  implicit final def showJsonObject[J: Show]: Show[JsonObject[J]] = Show.fromToString
  implicit final def eqJsonObject[J: Eq]: Eq[JsonObject[J]] = Eq.fromUniversalEquals

  /**
   * An implementation of [[JsonObject]] built on `java.util.LinkedHashMap`.
   */
  private[this] final class LinkedHashMapJsonObject[J](fields: LinkedHashMap[String, J]) extends JsonObject[J] {
    private[circe] def applyUnsafe(key: String): J = fields.get(key)
    final def apply(k: String): Option[J] = Option(fields.get(k))
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

    final def values: Iterable[J] = new Iterable[J] {
      final def iterator: Iterator[J] = new Iterator[J] {
        private[this] val underlying = fields.values.iterator

        final def hasNext: Boolean = underlying.hasNext
        final def next(): J = underlying.next()
      }
    }

    final def toMap: Map[String, J] = {
      val builder = Map.newBuilder[String, J]
      builder.sizeHint(size)

      val iterator = fields.entrySet.iterator

      while (iterator.hasNext) {
        val next = iterator.next

        builder += ((next.getKey, next.getValue))
      }

      builder.result()
    }

    final def toIterable: Iterable[(String, J)] = new Iterable[(String, J)] {
      final def iterator: Iterator[(String, J)] = new Iterator[(String, J)] {
        private[this] val underlying = fields.entrySet.iterator

        final def hasNext: Boolean = underlying.hasNext
        final def next(): (String, J) = {
          val field = underlying.next()

          (field.getKey, field.getValue)
        }
      }
    }

    final def appendToFolder(folder: Printer.PrintingFolder)(implicit ev: J <:< Json): Unit = {
      val originalDepth = folder.depth
      val p = folder.pieces(folder.depth)
      var first = true
      val iterable = if (folder.sortKeys) toIterable.toVector.sortBy(_._1) else toIterable
      val iterator = iterable.iterator

      folder.writer.append(p.lBraces)

      while (iterator.hasNext) {
        val next = iterator.next()
        val key = next._1
        val value = next._2

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

    private[this] def toMapAndVectorJsonObject: MapAndVectorJsonObject[J] = {
      val mapBuilder = Map.newBuilder[String, J]
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

    final def add(k: String, j: J): JsonObject[J] = toMapAndVectorJsonObject.add(k, j)
    final def +:(f: (String, J)): JsonObject[J] = toMapAndVectorJsonObject.+:(f)
    final def remove(k: String): JsonObject[J] = toMapAndVectorJsonObject.remove(k)

    final def traverse[F[_], K](f: J => F[K])(implicit F: Applicative[F]): F[JsonObject[K]] =
      toMapAndVectorJsonObject.traverse[F, K](f)(F)

    final def mapValues[K](f: J => K): JsonObject[K] = toMapAndVectorJsonObject.mapValues(f)
  }

  /**
   * A straightforward implementation of [[JsonObject]] with immutable collections.
   */
  private[this] final class MapAndVectorJsonObject[J](
    fields: Map[String, J],
    orderedKeys: Vector[String]
  ) extends JsonObject[J] {
    private[circe] def applyUnsafe(key: String): J = fields(key)
    final def apply(key: String): Option[J] = fields.get(key)
    final def size: Int = fields.size
    final def contains(key: String): Boolean = fields.contains(key)
    final def isEmpty: Boolean = fields.isEmpty

    final def keys: Iterable[String] = orderedKeys
    final def values: Iterable[J] = orderedKeys.map(key => fields(key))

    final def toMap: Map[String, J] = fields
    final def toIterable: Iterable[(String, J)] = orderedKeys.map(key => (key, fields(key)))

    final def add(key: String, value: J): JsonObject[J] =
      if (fields.contains(key)) {
        new MapAndVectorJsonObject(fields.updated(key, value), orderedKeys)
      } else {
        new MapAndVectorJsonObject(fields.updated(key, value), orderedKeys :+ key)
      }

    final def +:(field: (String, J)): JsonObject[J] = {
      val (key, value) = field
      if (fields.contains(key)) {
        new MapAndVectorJsonObject(fields.updated(key, value), orderedKeys)
      } else {
        new MapAndVectorJsonObject(fields.updated(key, value), key +: orderedKeys)
      }
    }

    final def remove(key: String): JsonObject[J] =
      new MapAndVectorJsonObject(fields - key, orderedKeys.filterNot(_ == key))

    final def traverse[F[_], K](f: J => F[K])(implicit F: Applicative[F]): F[JsonObject[K]] = F.map(
      orderedKeys.foldLeft(F.pure(Map.empty[String, K])) {
        case (acc, key) => F.map2(acc, f(fields(key)))(_.updated(key, _))
      }
    )(mappedFields => new MapAndVectorJsonObject(mappedFields, orderedKeys))

    final def mapValues[K](f: J => K): JsonObject[K] =
      new MapAndVectorJsonObject(
        fields.map {
          case (key, value) => (key, f(value))
        },
        orderedKeys
      )

    final def appendToFolder(folder: Printer.PrintingFolder)(implicit ev: J <:< Json): Unit = {
      val originalDepth = folder.depth
      val p = folder.pieces(folder.depth)
      var first = true
      val keyIterator = if (folder.sortKeys) orderedKeys.sorted.iterator else orderedKeys.iterator

      folder.writer.append(p.lBraces)

      while (keyIterator.hasNext) {
        val key = keyIterator.next()
        val value = fields(key).asInstanceOf[Json]
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
