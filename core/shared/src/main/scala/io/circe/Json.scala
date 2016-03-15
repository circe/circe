package io.circe

import algebra.Eq
import cats.Show
import io.circe.numbers.BiggerDecimal

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
   * The catamorphism for the JSON value data type.
   */
  final def fold[X](
    jsonNull: => X,
    jsonBoolean: Boolean => X,
    jsonNumber: JsonNumber => X,
    jsonString: String => X,
    jsonArray: List[Json] => X,
    jsonObject: JsonObject => X
  ): X = this match {
    case JNull       => jsonNull
    case JBoolean(b) => jsonBoolean(b)
    case JNumber(n)  => jsonNumber(n)
    case JString(s)  => jsonString(s)
    case JArray(a)   => jsonArray(a.toList)
    case JObject(o)  => jsonObject(o)
  }

  /**
   * Run on an array or object or return the given default.
   */
  final def arrayOrObject[X](
    or: => X,
    jsonArray: List[Json] => X,
    jsonObject: JsonObject => X
  ): X = this match {
    case JNull       => or
    case JBoolean(_) => or
    case JNumber(_)  => or
    case JString(_)  => or
    case JArray(a)   => jsonArray(a.toList)
    case JObject(o)  => jsonObject(o)
  }

  /**
   * Construct a cursor from this JSON value.
   */
  final def cursor: Cursor = Cursor(this)

  /**
   * Construct a cursor with history from this JSON value.
   */
  final def hcursor: HCursor = Cursor(this).hcursor

  def isNull: Boolean
  def isBoolean: Boolean
  def isNumber: Boolean
  def isString: Boolean
  def isArray: Boolean
  def isObject: Boolean

  def asBoolean: Option[Boolean]
  def asNumber: Option[JsonNumber]
  def asString: Option[String]
  def asArray: Option[List[Json]]
  def asObject: Option[JsonObject]

  final def withBoolean(f: Boolean => Json): Json = asBoolean.fold(this)(f)
  final def withNumber(f: JsonNumber => Json): Json = asNumber.fold(this)(f)
  final def withString(f: String => Json): Json = asString.fold(this)(f)
  final def withArray(f: List[Json] => Json): Json = asArray.fold(this)(f)
  final def withObject(f: JsonObject => Json): Json = asObject.fold(this)(f)

  def mapBoolean(f: Boolean => Boolean): Json
  def mapNumber(f: JsonNumber => JsonNumber): Json
  def mapString(f: String => String): Json
  def mapArray(f: List[Json] => List[Json]): Json
  def mapObject(f: JsonObject => JsonObject): Json

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
  final def as[A](implicit d: Decoder[A]): Decoder.Result[A] = d(cursor.hcursor)

  /**
   * Pretty-print this JSON value to a string using the given pretty-printer.
   */
  final def pretty(p: Printer): String = p.pretty(this)

  /**
   * Pretty-print this JSON value to a string with no spaces.
   */
  final def noSpaces: String = Printer.noSpaces.pretty(this)

  /**
   * Pretty-print this JSON value to a string indentation of two spaces.
   */
  final def spaces2: String = Printer.spaces2.pretty(this)

  /**
   * Pretty-print this JSON value to a string indentation of four spaces.
   */
  final def spaces4: String = Printer.spaces4.pretty(this)

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
          lhs.toList.foldLeft(rhs) {
            case (acc, (key, value)) =>
              rhs(key).fold(acc.add(key, value)) { r => acc.add(key, value.deepMerge(r)) }
          }
        )
      case _ => that
    }

  /**
   * Compute a `String` representation for this JSON value.
   */
  override final def toString: String = spaces2

  /**
   * Universal equality derived from our type-safe equality.
   */
  override final def equals(that: Any): Boolean = that match {
    case that: Json => Json.eqJson.eqv(this, that)
    case _ => false
  }

  /**
   * Use implementations provided by case classes.
   */
  override def hashCode(): Int
}

final object Json {
  private[circe] final case object JNull extends Json {
    final def isNull: Boolean = true
    final def isBoolean: Boolean = false
    final def isNumber: Boolean = false
    final def isString: Boolean = false
    final def isArray: Boolean = false
    final def isObject: Boolean = false

    final def asBoolean: Option[Boolean] = None
    final def asNumber: Option[JsonNumber] = None
    final def asString: Option[String] = None
    final def asArray: Option[List[Json]] = None
    final def asObject: Option[JsonObject] = None

    final def mapBoolean(f: Boolean => Boolean): Json = this
    final def mapNumber(f: JsonNumber => JsonNumber): Json = this
    final def mapString(f: String => String): Json = this
    final def mapArray(f: List[Json] => List[Json]): Json = this
    final def mapObject(f: JsonObject => JsonObject): Json = this
  }

  private[circe] final case class JBoolean(b: Boolean) extends Json {
    final def isNull: Boolean = false
    final def isBoolean: Boolean = true
    final def isNumber: Boolean = false
    final def isString: Boolean = false
    final def isArray: Boolean = false
    final def isObject: Boolean = false

    final def asBoolean: Option[Boolean] = Some(b)
    final def asNumber: Option[JsonNumber] = None
    final def asString: Option[String] = None
    final def asArray: Option[List[Json]] = None
    final def asObject: Option[JsonObject] = None

    final def mapBoolean(f: Boolean => Boolean): Json = JBoolean(f(b))
    final def mapNumber(f: JsonNumber => JsonNumber): Json = this
    final def mapString(f: String => String): Json = this
    final def mapArray(f: List[Json] => List[Json]): Json = this
    final def mapObject(f: JsonObject => JsonObject): Json = this
  }

  private[circe] final case class JNumber(n: JsonNumber) extends Json {
    final def isNull: Boolean = false
    final def isBoolean: Boolean = false
    final def isNumber: Boolean = true
    final def isString: Boolean = false
    final def isArray: Boolean = false
    final def isObject: Boolean = false

    final def asBoolean: Option[Boolean] = None
    final def asNumber: Option[JsonNumber] = Some(n)
    final def asString: Option[String] = None
    final def asArray: Option[List[Json]] = None
    final def asObject: Option[JsonObject] = None

    final def mapBoolean(f: Boolean => Boolean): Json = this
    final def mapNumber(f: JsonNumber => JsonNumber): Json = JNumber(f(n))
    final def mapString(f: String => String): Json = this
    final def mapArray(f: List[Json] => List[Json]): Json = this
    final def mapObject(f: JsonObject => JsonObject): Json = this
  }

  private[circe] final case class JString(s: String) extends Json {
    final def isNull: Boolean = false
    final def isBoolean: Boolean = false
    final def isNumber: Boolean = false
    final def isString: Boolean = true
    final def isArray: Boolean = false
    final def isObject: Boolean = false

    final def asBoolean: Option[Boolean] = None
    final def asNumber: Option[JsonNumber] = None
    final def asString: Option[String] = Some(s)
    final def asArray: Option[List[Json]] = None
    final def asObject: Option[JsonObject] = None

    final def mapBoolean(f: Boolean => Boolean): Json = this
    final def mapNumber(f: JsonNumber => JsonNumber): Json = this
    final def mapString(f: String => String): Json = JString(f(s))
    final def mapArray(f: List[Json] => List[Json]): Json = this
    final def mapObject(f: JsonObject => JsonObject): Json = this
  }

  private[circe] final case class JArray(a: Seq[Json]) extends Json {
    final def isNull: Boolean = false
    final def isBoolean: Boolean = false
    final def isNumber: Boolean = false
    final def isString: Boolean = false
    final def isArray: Boolean = true
    final def isObject: Boolean = false

    final def asBoolean: Option[Boolean] = None
    final def asNumber: Option[JsonNumber] = None
    final def asString: Option[String] = None
    final def asArray: Option[List[Json]] = Some(a.toList)
    final def asObject: Option[JsonObject] = None

    final def mapBoolean(f: Boolean => Boolean): Json = this
    final def mapNumber(f: JsonNumber => JsonNumber): Json = this
    final def mapString(f: String => String): Json = this
    final def mapArray(f: List[Json] => List[Json]): Json = JArray(f(a.toList))
    final def mapObject(f: JsonObject => JsonObject): Json = this
  }

  private[circe] final case class JObject(o: JsonObject) extends Json {
    final def isNull: Boolean = false
    final def isBoolean: Boolean = false
    final def isNumber: Boolean = false
    final def isString: Boolean = false
    final def isArray: Boolean = false
    final def isObject: Boolean = true

    final def asBoolean: Option[Boolean] = None
    final def asNumber: Option[JsonNumber] = None
    final def asString: Option[String] = None
    final def asArray: Option[List[Json]] = None
    final def asObject: Option[JsonObject] = Some(o)

    final def mapBoolean(f: Boolean => Boolean): Json = this
    final def mapNumber(f: JsonNumber => JsonNumber): Json = this
    final def mapString(f: String => String): Json = this
    final def mapArray(f: List[Json] => List[Json]): Json = this
    final def mapObject(f: JsonObject => JsonObject): Json = JObject(f(o))
  }

  final val Null: Json = JNull
  final val True: Json = JBoolean(true)
  final val False: Json = JBoolean(false)

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
  final def fromValues(values: Iterable[Json]): Json = JArray(values.toList)

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
  final def fromString(value: String): Json = JString(value)

  /**
   * Create a `Json` value representing a JSON boolean.
   */
  final def fromBoolean(value: Boolean): Json = if (value) True else False

  /**
   * Create a `Json` value representing a JSON number from an `Int`.
   */
  final def fromInt(value: Int): Json = JNumber(JsonLong(value.toLong))

  /**
   * Create a `Json` value representing a JSON number from a `Long`.
   */
  final def fromLong(value: Long): Json = JNumber(JsonLong(value))

  /**
   * Try to create a `Json` value representing a JSON number from a `Double`.
   *
   * The result is empty if the argument cannot be represented as a JSON number.
   */
  final def fromDouble(value: Double): Option[Json] = if (isReal(value)) Some(JNumber(JsonDouble(value))) else None

  /**
   * Create a `Json` value representing a JSON number or null from a `Double`.
   *
   * The result is a JSON null if the argument cannot be represented as a JSON
   * number.
   */
  final def fromDoubleOrNull(value: Double): Json = if (isReal(value)) JNumber(JsonDouble(value)) else Null

  /**
   * Create a `Json` value representing a JSON number or string from a `Double`.
   *
   * The result is a JSON string if the argument cannot be represented as a JSON
   * number.
   */
  final def fromDoubleOrString(value: Double): Json =
    if (isReal(value)) JNumber(JsonDouble(value)) else fromString(value.toString)

  /**
   * Create a `Json` value representing a JSON number from a `BigInt`.
   */
  final def fromBigInt(value: BigInt): Json = JNumber(JsonBiggerDecimal(BiggerDecimal.fromBigInteger(value.underlying)))

  /**
   * Create a `Json` value representing a JSON number from a `BigDecimal`.
   */
  final def fromBigDecimal(value: BigDecimal): Json = JNumber(JsonBigDecimal(value))

  private[this] def isReal(value: Double): Boolean = !value.isNaN && !value.isInfinity

  @deprecated("Use Null", "0.4.0")
  final val Empty: Json = Null

  @deprecated("Use Null", "0.4.0")
  final def empty: Json = Null

  @deprecated("Use fromBoolean", "0.4.0")
  final def bool(b: Boolean): Json = fromBoolean(b)

  @deprecated("Use fromInt", "0.4.0")
  final def int(n: Int): Json = fromInt(n)

  @deprecated("Use fromLong", "0.4.0")
  final def long(n: Long): Json = fromLong(n)

  @deprecated("Use fromBigDecimal", "0.4.0")
  final def bigDecimal(n: BigDecimal): Json = fromBigDecimal(n)

  @deprecated("Use fromDouble", "0.4.0")
  final def number(n: Double): Option[Json] = fromDouble(n)

  @deprecated("Use fromDoubleOrNull", "0.4.0")
  final def numberOrNull(n: Double): Json = fromDoubleOrNull(n)

  @deprecated("Use fromDoubleOrString", "0.4.0")
  final def numberOrString(n: Double): Json = fromDoubleOrString(n)

  @deprecated("Use fromString", "0.4.0")
  final def string(s: String): Json = JString(s)

  @deprecated("Use arr", "0.4.0")
  final def array(values: Json*): Json = arr(values: _*)

  private[this] final def arrayEq(x: Seq[Json], y: Seq[Json]): Boolean = {
    val it0 = x.iterator
    val it1 = y.iterator
    while (it0.hasNext && it1.hasNext) {
      if (Json.eqJson.neqv(it0.next, it1.next)) return false
    }
    it0.hasNext == it1.hasNext
  }

  implicit final val eqJson: Eq[Json] = Eq.instance {
    case ( JObject(a),  JObject(b)) => JsonObject.eqJsonObject.eqv(a, b)
    case ( JString(a),  JString(b)) => a == b
    case ( JNumber(a),  JNumber(b)) => JsonNumber.eqJsonNumber.eqv(a, b)
    case (JBoolean(a), JBoolean(b)) => a == b
    case (  JArray(a),   JArray(b)) => arrayEq(a, b)
    case (          x,           y) => x.isNull && y.isNull
  }

  implicit final val showJson: Show[Json] = Show.fromToString[Json]
}
