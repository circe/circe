/*
 * Copyright 2024 circe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.circe

import cats.Applicative
import cats.Eq
import cats.Show
import io.circe.numbers.BiggerDecimal

import java.io.Serializable

import scala.collection.mutable.ListBuffer

/**
 * A data type representing possible JSON values.
 *
 * @author Travis Brown
 * @author Tony Morris
 * @author Dylan Just
 * @author Mark Hibberd
 */
sealed abstract class Json extends Product with Serializable {
  import Json._

  /**
   * Reduce this JSON value with the given [[Json.Folder]].
   */
  def foldWith[X](folder: Json.Folder[X]): X

  /**
   * The catamorphism for the JSON value data type.
   */
  final def fold[X](
    jsonNull: => X,
    jsonBoolean: Boolean => X,
    jsonNumber: JsonNumber => X,
    jsonString: String => X,
    jsonArray: Vector[Json] => X,
    jsonObject: JsonObject => X
  ): X = this match {
    case JNull       => jsonNull
    case JBoolean(b) => jsonBoolean(b)
    case JNumber(n)  => jsonNumber(n)
    case JString(s)  => jsonString(s)
    case JArray(a)   => jsonArray(a)
    case JObject(o)  => jsonObject(o)
  }

  /**
   * Run on an array or object or return the given default.
   */
  final def arrayOrObject[X](
    or: => X,
    jsonArray: Vector[Json] => X,
    jsonObject: JsonObject => X
  ): X = this match {
    case JNull       => or
    case JBoolean(_) => or
    case JNumber(_)  => or
    case JString(_)  => or
    case JArray(a)   => jsonArray(a)
    case JObject(o)  => jsonObject(o)
  }

  /**
   * Construct a successful cursor from this JSON value.
   */
  final def hcursor: HCursor = HCursor.fromJson(this)

  def isNull: Boolean
  def isBoolean: Boolean
  def isNumber: Boolean
  def isString: Boolean
  def isArray: Boolean
  def isObject: Boolean

  def asNull: Option[Unit]
  def asBoolean: Option[Boolean]
  def asNumber: Option[JsonNumber]
  def asString: Option[String]
  def asArray: Option[Vector[Json]]
  def asObject: Option[JsonObject]

  def withNull(f: => Json): Json
  def withBoolean(f: Boolean => Json): Json
  def withNumber(f: JsonNumber => Json): Json
  def withString(f: String => Json): Json
  def withArray(f: Vector[Json] => Json): Json
  def withObject(f: JsonObject => Json): Json

  def mapBoolean(f: Boolean => Boolean): Json
  def mapNumber(f: JsonNumber => JsonNumber): Json
  def mapString(f: String => String): Json
  def mapArray(f: Vector[Json] => Vector[Json]): Json
  def mapObject(f: JsonObject => JsonObject): Json

  def traverseBoolean[F[_]](f: Boolean => F[Json])(implicit F: Applicative[F]): F[Json]
  def traverseNumber[F[_]](f: JsonNumber => F[Json])(implicit F: Applicative[F]): F[Json]
  def traverseString[F[_]](f: String => F[Json])(implicit F: Applicative[F]): F[Json]
  def traverseArray[F[_]](f: Vector[Json] => F[Json])(implicit F: Applicative[F]): F[Json]
  def traverseObject[F[_]](f: JsonObject => F[Json])(implicit F: Applicative[F]): F[Json]

  /**
   * The name of the type of the JSON value.
   */
  final def name: String =
    this match {
      case JNull       => "Null"
      case JBoolean(_) => "Boolean"
      case JNumber(_)  => "Number"
      case JString(_)  => "String"
      case JArray(_)   => "Array"
      case JObject(_)  => "Object"
    }

  /**
   * Attempts to decode this JSON value to another data type.
   */
  final def as[A](implicit d: Decoder[A]): Decoder.Result[A] = d(hcursor)

  /**
   * Attempts to decode this JSON value to another data type, accumulating failures.
   */
  final def asAccumulating[A](implicit d: Decoder[A]): Decoder.AccumulatingResult[A] = d.decodeAccumulating(hcursor)

  /**
   * Pretty-print this JSON value to a string using the given pretty-printer.
   */
  final def printWith(p: Printer): String = p.print(this)

  /**
   * Pretty-print this JSON value to a string with no spaces.
   */
  final def noSpaces: String = Printer.noSpaces.print(this)

  /**
   * Pretty-print this JSON value to a string indentation of two spaces.
   */
  final def spaces2: String = Printer.spaces2.print(this)

  /**
   * Pretty-print this JSON value to a string indentation of four spaces.
   */
  final def spaces4: String = Printer.spaces4.print(this)

  /**
   * Pretty-print this JSON value to a string with no spaces, with object keys
   * sorted alphabetically.
   */
  final def noSpacesSortKeys: String = Printer.noSpacesSortKeys.print(this)

  /**
   * Pretty-print this JSON value to a string indentation of two spaces, with
   * object keys sorted alphabetically.
   */
  final def spaces2SortKeys: String = Printer.spaces2SortKeys.print(this)

  /**
   * Pretty-print this JSON value to a string indentation of four spaces, with
   * object keys sorted alphabetically.
   */
  final def spaces4SortKeys: String = Printer.spaces4SortKeys.print(this)

  /**
   * Perform a deep merge of this JSON value with another JSON value.
   *
   * Objects are merged by key, values from the argument JSON take
   * precedence over values from this JSON. Nested objects are
   * recursed.
   *
   * Null, Array, Boolean, String and Number are treated as values,
   * and values from the argument JSON completely replace values
   * from this JSON.
   */
  def deepMerge(that: Json): Json =
    (asObject, that.asObject) match {
      case (Some(lhs), Some(rhs)) =>
        fromJsonObject(
          lhs.toIterable.foldLeft(rhs) {
            case (acc, (key, value)) =>
              rhs(key).fold(acc.add(key, value)) { r =>
                acc.add(key, value.deepMerge(r))
              }
          }
        )
      case _ => that
    }

  /**
   * Drop the entries with a null value if this is an object.
   *
   * Note that this does not apply recursively.
   */
  def dropNullValues: Json = this.mapObject(_.filter { case (_, v) => !v.isNull })

  /**
   * Drop the entries with a null value if this is an object or array.
   */
  def deepDropNullValues: Json = {
    val folder = new Json.Folder[Json] {
      def onNull: Json = Json.Null
      def onBoolean(value: Boolean): Json = Json.fromBoolean(value)
      def onNumber(value: JsonNumber): Json = Json.fromJsonNumber(value)
      def onString(value: String): Json = Json.fromString(value)
      def onArray(value: Vector[Json]): Json =
        Json.fromValues(value.collect {
          case v if !v.isNull => v.foldWith(this)
        })
      def onObject(value: JsonObject): Json =
        Json.fromJsonObject(
          value.filter { case (_, v) => !v.isNull }.mapValues(_.foldWith(this))
        )
    }

    this.foldWith(folder)
  }

  /**
   * Drop the entries with an empty value if this is an array or object.
   *
   * Note that this does not apply recursively.
   */
  def dropEmptyValues: Json = this.mapObject(_.filter {
    case (_, JArray(vec))  => vec.nonEmpty
    case (_, JObject(obj)) => obj.nonEmpty
    case _                 => true
  })

  /**
   * Compute a `String` representation for this JSON value.
   */
  override final def toString: String = spaces2

  /**
   * Universal equality derived from our type-safe equality.
   */
  override final def equals(that: Any): Boolean = that match {
    case that: Json => Json.eqJson.eqv(this, that)
    case _          => false
  }

  /**
   * Use implementations provided by case classes.
   */
  def hashCode(): Int

  // Alias for `findAllByKey`.
  final def \\(key: String): List[Json] = findAllByKey(key)

  /**
   * Recursively return all values matching the specified `key`.
   *
   * The Play docs, from which this method was inspired, reads:
   *   "Lookup for fieldName in the current object and all descendants."
   */
  final def findAllByKey(key: String): List[Json] = {
    val hh: ListBuffer[Json] = ListBuffer.empty[Json]
    def loop(json: Json): Unit = json match {
      case JObject(obj) =>
        obj.toIterable.foreach {
          case (k, v) =>
            if (k == key) hh += v
            loop(v)
        }
      case JArray(elems) => elems.foreach(loop)
      case _             => // do nothing
    }
    loop(this)
    hh.toList
  }
}

object Json {

  /**
   * Represents a set of operations for reducing a [[Json]] instance to a value.
   */
  trait Folder[X] extends Serializable {
    def onNull: X
    def onBoolean(value: Boolean): X
    def onNumber(value: JsonNumber): X
    def onString(value: String): X
    def onArray(value: Vector[Json]): X
    def onObject(value: JsonObject): X
  }

  private[circe] case object JNull extends Json {
    final def foldWith[X](folder: Folder[X]): X = folder.onNull

    final def isNull: Boolean = true
    final def isBoolean: Boolean = false
    final def isNumber: Boolean = false
    final def isString: Boolean = false
    final def isArray: Boolean = false
    final def isObject: Boolean = false

    final def asNull: Option[Unit] = Some(())
    final def asBoolean: Option[Boolean] = None
    final def asNumber: Option[JsonNumber] = None
    final def asString: Option[String] = None
    final def asArray: Option[Vector[Json]] = None
    final def asObject: Option[JsonObject] = None

    final def withNull(f: => Json): Json = f
    final def withBoolean(f: Boolean => Json): Json = this
    final def withNumber(f: JsonNumber => Json): Json = this
    final def withString(f: String => Json): Json = this
    final def withArray(f: Vector[Json] => Json): Json = this
    final def withObject(f: JsonObject => Json): Json = this

    final def mapBoolean(f: Boolean => Boolean): Json = this
    final def mapNumber(f: JsonNumber => JsonNumber): Json = this
    final def mapString(f: String => String): Json = this
    final def mapArray(f: Vector[Json] => Vector[Json]): Json = this
    final def mapObject(f: JsonObject => JsonObject): Json = this

    final def traverseBoolean[F[_]](f: Boolean => F[Json])(implicit F: Applicative[F]): F[Json] = F.pure(this)
    final def traverseNumber[F[_]](f: JsonNumber => F[Json])(implicit F: Applicative[F]): F[Json] = F.pure(this)
    final def traverseString[F[_]](f: String => F[Json])(implicit F: Applicative[F]): F[Json] = F.pure(this)
    final def traverseArray[F[_]](f: Vector[Json] => F[Json])(implicit F: Applicative[F]): F[Json] =
      F.pure(this)
    final def traverseObject[F[_]](f: JsonObject => F[Json])(implicit F: Applicative[F]): F[Json] = F.pure(this)
  }

  private[circe] final case class JBoolean(value: Boolean) extends Json {
    final def foldWith[X](folder: Folder[X]): X = folder.onBoolean(value)

    final def isNull: Boolean = false
    final def isBoolean: Boolean = true
    final def isNumber: Boolean = false
    final def isString: Boolean = false
    final def isArray: Boolean = false
    final def isObject: Boolean = false

    final def asNull: Option[Unit] = None
    final def asBoolean: Option[Boolean] = Some(value)
    final def asNumber: Option[JsonNumber] = None
    final def asString: Option[String] = None
    final def asArray: Option[Vector[Json]] = None
    final def asObject: Option[JsonObject] = None

    final def withNull(f: => Json): Json = this
    final def withBoolean(f: Boolean => Json): Json = f(value)
    final def withNumber(f: JsonNumber => Json): Json = this
    final def withString(f: String => Json): Json = this
    final def withArray(f: Vector[Json] => Json): Json = this
    final def withObject(f: JsonObject => Json): Json = this

    final def mapBoolean(f: Boolean => Boolean): Json = JBoolean(f(value))
    final def mapNumber(f: JsonNumber => JsonNumber): Json = this
    final def mapString(f: String => String): Json = this
    final def mapArray(f: Vector[Json] => Vector[Json]): Json = this
    final def mapObject(f: JsonObject => JsonObject): Json = this

    final def traverseBoolean[F[_]](f: Boolean => F[Json])(implicit F: Applicative[F]): F[Json] = f(value)
    final def traverseNumber[F[_]](f: JsonNumber => F[Json])(implicit F: Applicative[F]): F[Json] = F.pure(this)
    final def traverseString[F[_]](f: String => F[Json])(implicit F: Applicative[F]): F[Json] = F.pure(this)
    final def traverseArray[F[_]](f: Vector[Json] => F[Json])(implicit F: Applicative[F]): F[Json] = F.pure(this)
    final def traverseObject[F[_]](f: JsonObject => F[Json])(implicit F: Applicative[F]): F[Json] = F.pure(this)
  }

  private[circe] final case class JNumber(value: JsonNumber) extends Json {
    final def foldWith[X](folder: Folder[X]): X = folder.onNumber(value)

    final def isNull: Boolean = false
    final def isBoolean: Boolean = false
    final def isNumber: Boolean = true
    final def isString: Boolean = false
    final def isArray: Boolean = false
    final def isObject: Boolean = false

    final def asNull: Option[Unit] = None
    final def asBoolean: Option[Boolean] = None
    final def asNumber: Option[JsonNumber] = Some(value)
    final def asString: Option[String] = None
    final def asArray: Option[Vector[Json]] = None
    final def asObject: Option[JsonObject] = None

    final def withNull(f: => Json): Json = this
    final def withBoolean(f: Boolean => Json): Json = this
    final def withNumber(f: JsonNumber => Json): Json = f(value)
    final def withString(f: String => Json): Json = this
    final def withArray(f: Vector[Json] => Json): Json = this
    final def withObject(f: JsonObject => Json): Json = this

    final def mapBoolean(f: Boolean => Boolean): Json = this
    final def mapNumber(f: JsonNumber => JsonNumber): Json = JNumber(f(value))
    final def mapString(f: String => String): Json = this
    final def mapArray(f: Vector[Json] => Vector[Json]): Json = this
    final def mapObject(f: JsonObject => JsonObject): Json = this

    final def traverseBoolean[F[_]](f: Boolean => F[Json])(implicit F: Applicative[F]): F[Json] = F.pure(this)
    final def traverseNumber[F[_]](f: JsonNumber => F[Json])(implicit F: Applicative[F]): F[Json] = f(value)
    final def traverseString[F[_]](f: String => F[Json])(implicit F: Applicative[F]): F[Json] = F.pure(this)
    final def traverseArray[F[_]](f: Vector[Json] => F[Json])(implicit F: Applicative[F]): F[Json] = F.pure(this)
    final def traverseObject[F[_]](f: JsonObject => F[Json])(implicit F: Applicative[F]): F[Json] = F.pure(this)
  }

  private[circe] final case class JString(value: String) extends Json {
    final def foldWith[X](folder: Folder[X]): X = folder.onString(value)

    final def isNull: Boolean = false
    final def isBoolean: Boolean = false
    final def isNumber: Boolean = false
    final def isString: Boolean = true
    final def isArray: Boolean = false
    final def isObject: Boolean = false

    final def asNull: Option[Unit] = None
    final def asBoolean: Option[Boolean] = None
    final def asNumber: Option[JsonNumber] = None
    final def asString: Option[String] = Some(value)
    final def asArray: Option[Vector[Json]] = None
    final def asObject: Option[JsonObject] = None

    final def withNull(f: => Json): Json = this
    final def withBoolean(f: Boolean => Json): Json = this
    final def withNumber(f: JsonNumber => Json): Json = this
    final def withString(f: String => Json): Json = f(value)
    final def withArray(f: Vector[Json] => Json): Json = this
    final def withObject(f: JsonObject => Json): Json = this

    final def mapBoolean(f: Boolean => Boolean): Json = this
    final def mapNumber(f: JsonNumber => JsonNumber): Json = this
    final def mapString(f: String => String): Json = JString(f(value))
    final def mapArray(f: Vector[Json] => Vector[Json]): Json = this
    final def mapObject(f: JsonObject => JsonObject): Json = this

    final def traverseBoolean[F[_]](f: Boolean => F[Json])(implicit F: Applicative[F]): F[Json] = F.pure(this)
    final def traverseNumber[F[_]](f: JsonNumber => F[Json])(implicit F: Applicative[F]): F[Json] = F.pure(this)
    final def traverseString[F[_]](f: String => F[Json])(implicit F: Applicative[F]): F[Json] = f(value)
    final def traverseArray[F[_]](f: Vector[Json] => F[Json])(implicit F: Applicative[F]): F[Json] = F.pure(this)
    final def traverseObject[F[_]](f: JsonObject => F[Json])(implicit F: Applicative[F]): F[Json] = F.pure(this)
  }

  private[circe] final case class JArray(value: Vector[Json]) extends Json {
    final def foldWith[X](folder: Folder[X]): X = folder.onArray(value)

    final def isNull: Boolean = false
    final def isBoolean: Boolean = false
    final def isNumber: Boolean = false
    final def isString: Boolean = false
    final def isArray: Boolean = true
    final def isObject: Boolean = false

    final def asNull: Option[Unit] = None
    final def asBoolean: Option[Boolean] = None
    final def asNumber: Option[JsonNumber] = None
    final def asString: Option[String] = None
    final def asArray: Option[Vector[Json]] = Some(value)
    final def asObject: Option[JsonObject] = None

    final def withNull(f: => Json): Json = this
    final def withBoolean(f: Boolean => Json): Json = this
    final def withNumber(f: JsonNumber => Json): Json = this
    final def withString(f: String => Json): Json = this
    final def withArray(f: Vector[Json] => Json): Json = f(value)
    final def withObject(f: JsonObject => Json): Json = this

    final def mapBoolean(f: Boolean => Boolean): Json = this
    final def mapNumber(f: JsonNumber => JsonNumber): Json = this
    final def mapString(f: String => String): Json = this
    final def mapArray(f: Vector[Json] => Vector[Json]): Json = JArray(f(value))
    final def mapObject(f: JsonObject => JsonObject): Json = this

    final def traverseBoolean[F[_]](f: Boolean => F[Json])(implicit F: Applicative[F]): F[Json] = F.pure(this)
    final def traverseNumber[F[_]](f: JsonNumber => F[Json])(implicit F: Applicative[F]): F[Json] = F.pure(this)
    final def traverseString[F[_]](f: String => F[Json])(implicit F: Applicative[F]): F[Json] = F.pure(this)
    final def traverseArray[F[_]](f: Vector[Json] => F[Json])(implicit F: Applicative[F]): F[Json] = f(value)
    final def traverseObject[F[_]](f: JsonObject => F[Json])(implicit F: Applicative[F]): F[Json] = F.pure(this)
  }

  private[circe] final case class JObject(value: JsonObject) extends Json {
    final def foldWith[X](folder: Folder[X]): X = folder.onObject(value)

    final def isNull: Boolean = false
    final def isBoolean: Boolean = false
    final def isNumber: Boolean = false
    final def isString: Boolean = false
    final def isArray: Boolean = false
    final def isObject: Boolean = true

    final def asNull: Option[Unit] = None
    final def asBoolean: Option[Boolean] = None
    final def asNumber: Option[JsonNumber] = None
    final def asString: Option[String] = None
    final def asArray: Option[Vector[Json]] = None
    final def asObject: Option[JsonObject] = Some(value)

    final def withNull(f: => Json): Json = this
    final def withBoolean(f: Boolean => Json): Json = this
    final def withNumber(f: JsonNumber => Json): Json = this
    final def withString(f: String => Json): Json = this
    final def withArray(f: Vector[Json] => Json): Json = this
    final def withObject(f: JsonObject => Json): Json = f(value)

    final def mapBoolean(f: Boolean => Boolean): Json = this
    final def mapNumber(f: JsonNumber => JsonNumber): Json = this
    final def mapString(f: String => String): Json = this
    final def mapArray(f: Vector[Json] => Vector[Json]): Json = this
    final def mapObject(f: JsonObject => JsonObject): Json = JObject(f(value))

    final def traverseBoolean[F[_]](f: Boolean => F[Json])(implicit F: Applicative[F]): F[Json] = F.pure(this)
    final def traverseNumber[F[_]](f: JsonNumber => F[Json])(implicit F: Applicative[F]): F[Json] = F.pure(this)
    final def traverseString[F[_]](f: String => F[Json])(implicit F: Applicative[F]): F[Json] = F.pure(this)
    final def traverseArray[F[_]](f: Vector[Json] => F[Json])(implicit F: Applicative[F]): F[Json] = F.pure(this)
    final def traverseObject[F[_]](f: JsonObject => F[Json])(implicit F: Applicative[F]): F[Json] = f(value)
  }

  final val Null: Json = JNull
  final val True: Json = JBoolean(true)
  final val False: Json = JBoolean(false)
  private[this] final val EmptyArray: Json = JArray(Vector.empty)
  private[this] final val EmptyString: Json = JString("")

  /**
   * Create a `Json` value representing a JSON object from key-value pairs.
   */
  final def obj(fields: (String, Json)*): Json = fromFields(fields)

  /**
   * Create a `Json` value representing a JSON array from values.
   */
  final def arr(values: Json*): Json = fromValues(values)

  /**
   * Create a `Json` value representing a JSON object from a collection of key-value pairs.
   */
  final def fromFields(fields: Iterable[(String, Json)]): Json = JObject(JsonObject.fromIterable(fields))

  /**
   * Create a `Json` value representing a JSON array from a collection of values.
   */
  final def fromValues(values: Iterable[Json]): Json =
    if (values.isEmpty) EmptyArray
    else JArray(values.toVector)

  /**
   * Create a `Json` value representing a JSON object from a [[JsonObject]].
   */
  final def fromJsonObject(value: JsonObject): Json = JObject(value)

  /**
   * Create a `Json` value representing a JSON number from a [[JsonNumber]].
   */
  final def fromJsonNumber(value: JsonNumber): Json = JNumber(value)

  /**
   * Create a `Json` value representing a JSON string.
   *
   * Note that this does not parse the argument.
   */
  final def fromString(value: String): Json =
    if (value.isEmpty) EmptyString
    else JString(value)

  /**
   * Create a `Json` value representing a JSON String or null from an `Option[String]`.
   *
   * Note that this does not parse the argument.
   */
  final def fromStringOrNull(value: Option[String]): Json = value.fold(Json.Null)(fromString)

  /**
   * Create a `Json` value representing a JSON boolean.
   */
  final def fromBoolean(value: Boolean): Json = if (value) True else False

  /**
   * Create a `Json` value representing a JSON boolean or null from an `Option[Boolean]`.
   */
  final def fromBooleanOrNull(value: Option[Boolean]): Json = value.fold(Json.Null)(fromBoolean)

  /**
   * Create a `Json` value representing a JSON number from an `Int`.
   */
  final def fromInt(value: Int): Json = JsonNumber.fromLong(value.toLong)

  /**
   * Create a `Json` value representing a JSON number or a null from an `Option[Int]`.
   */
  final def fromIntOrNull(value: Option[Int]): Json = value.fold(Json.Null)(fromInt)

  /**
   * Create a `Json` value representing a JSON number from a `Long`.
   */
  final def fromLong(value: Long): Json = JsonNumber.fromLong(value)

  /**
   * Create a `Json` value representing a JSON number or null from an optional `Long`.
   */
  final def fromLongOrNull(value: Option[Long]): Json = value.fold(Json.Null)(fromLong)

  /**
   * Try to create a `Json` value representing a JSON number from a `Double`.
   *
   * The result is empty if the argument cannot be represented as a JSON number.
   */
  final def fromDouble(value: Double): Option[Json] = if (isReal(value)) Some(JNumber(JsonDouble(value))) else None

  /**
   * Try to create a `Json` value representing a JSON number from a `Float`.
   *
   * The result is empty if the argument cannot be represented as a JSON number.
   */
  final def fromFloat(value: Float): Option[Json] = if (isReal(value)) Some(JNumber(JsonFloat(value))) else None

  /**
   * Create a `Json` value representing a JSON number or null from an optional `Double`
   */
  final def fromDoubleOrNull(value: Option[Double]): Json = value.fold(Json.Null)(fromDoubleOrNull)

  /**
   * Create a `Json` value representing a JSON number or null from a `Double`.
   *
   * The result is a JSON null if the argument cannot be represented as a JSON
   * number.
   */
  final def fromDoubleOrNull(value: Double): Json = if (isReal(value)) JNumber(JsonDouble(value)) else Null

  /**
   * Create a `Json` value representing a JSON number or null from a `Float`.
   *
   * The result is a JSON null if the argument cannot be represented as a JSON
   * number.
   */
  final def fromFloatOrNull(value: Float): Json = if (isReal(value)) JNumber(JsonFloat(value)) else Null

  /**
   * Create a `Json` value representing a JSON number or null from an optional `Float`
   */
  final def fromFloatOrNull(value: Option[Float]): Json = value.fold(Json.Null)(fromFloatOrNull)

  /**
   * Create a `Json` value representing a JSON number or string from a `Double`.
   *
   * The result is a JSON string if the argument cannot be represented as a JSON
   * number.
   */
  final def fromDoubleOrString(value: Double): Json =
    if (isReal(value)) JNumber(JsonDouble(value)) else fromString(java.lang.Double.toString(value))

  /**
   * Create a `Json` value representing a JSON number or string from a `Float`.
   *
   * The result is a JSON string if the argument cannot be represented as a JSON
   * number.
   */
  final def fromFloatOrString(value: Float): Json =
    if (isReal(value)) JNumber(JsonFloat(value)) else fromString(java.lang.Float.toString(value))

  /**
   * Create a `Json` value representing a JSON number from a `BigInt`.
   */
  final def fromBigInt(value: BigInt): Json = JNumber(
    JsonBiggerDecimal(BiggerDecimal.fromBigInteger(value.underlying), value.toString)
  )

  /**
   * Create a `Json` value representing a JSON number or null from an `Option[BigInt]`.
   */
  final def fromBigIntOrNull(value: Option[BigInt]): Json = value.fold(Json.Null)(fromBigInt)

  /**
   * Create a `Json` value representing a JSON number from a `BigDecimal`.
   */
  final def fromBigDecimal(value: BigDecimal): Json = JNumber(JsonBigDecimal(value.underlying))

  /**
   * Create a `Json` value representing a JSON number or null from an `Option[BigDecimal]`.
   */
  final def fromBigDecimalOrNull(value: Option[BigDecimal]): Json = value.fold(Json.Null)(fromBigDecimal)

  /**
   * Calling `.isFinite` directly on the value boxes; we explicitly avoid that here.
   */
  private[this] def isReal(value: Double): Boolean = java.lang.Double.isFinite(value)

  /**
   * Calling `.isFinite` directly on the value boxes; we explicitly avoid that here.
   */
  private[this] def isReal(value: Float): Boolean = java.lang.Float.isFinite(value)

  private[this] final def arrayEq(x: Vector[Json], y: Vector[Json]): Boolean = {
    if (x.length != y.length) return false
    val it0 = x.iterator
    val it1 = y.iterator
    while (it0.hasNext) {
      if (Json.eqJson.neqv(it0.next(), it1.next())) return false
    }
    true
  }

  implicit final val eqJson: Eq[Json] = Eq.instance {
    case (JObject(a), JObject(b))   => JsonObject.eqJsonObject.eqv(a, b)
    case (JString(a), JString(b))   => a == b
    case (JNumber(a), JNumber(b))   => JsonNumber.eqJsonNumber.eqv(a, b)
    case (JBoolean(a), JBoolean(b)) => a == b
    case (JArray(a), JArray(b))     => arrayEq(a, b)
    case (x, y)                     => x.isNull && y.isNull
  }

  implicit final val showJson: Show[Json] = Show.fromToString[Json]
}
