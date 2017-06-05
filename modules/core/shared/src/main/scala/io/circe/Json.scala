package io.circe

import cats.{ Eq, Show }
import io.circe.numbers.JsonNumber

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
  def foldWith[Z](folder: Json.Folder[Z]): Z

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
  ): X = foldWith(
    new Json.Folder[X] {
      def onNull: X = jsonNull
      def onBoolean(value: Boolean): X = jsonBoolean(value)
      def onNumber(value: JsonNumber): X = jsonNumber(value)
      def onDouble(value: Double): X = jsonNumber(JsonNumber.fromDouble(value))
      def onFloat(value: Float): X = jsonNumber(JsonNumber.fromFloat(value))
      def onLong(value: Long): X = jsonNumber(JsonNumber.fromLong(value))
      def onString(value: String): X = jsonString(value)
      def onArray(value: Vector[Json]): X = jsonArray(value)
      def onObject(value: JsonObject): X = jsonObject(value)
    }
  )

  /**
   * Run on an array or object or return the given default.
   */
  final def arrayOrObject[X](
    or: => X,
    jsonArray: Vector[Json] => X,
    jsonObject: JsonObject => X
  ): X = foldWith(
    new Json.Folder[X] {
      def onNull: X = or
      def onBoolean(value: Boolean): X = or
      def onNumber(value: JsonNumber): X = or
      def onDouble(value: Double): X = or
      def onFloat(value: Float): X = or
      def onLong(value: Long): X = or
      def onString(value: String): X = or
      def onArray(value: Vector[Json]): X = jsonArray(value)
      def onObject(value: JsonObject): X = jsonObject(value)
    }
  )

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

  def asBoolean: Option[Boolean]
  def asNumber: Option[JsonNumber]
  def asString: Option[String]
  def asArray: Option[Vector[Json]]
  def asObject: Option[JsonObject]

  final def withBoolean(f: Boolean => Json): Json = asBoolean.fold(this)(f)
  final def withNumber(f: JsonNumber => Json): Json = asNumber.fold(this)(f)
  final def withString(f: String => Json): Json = asString.fold(this)(f)
  final def withArray(f: Vector[Json] => Json): Json = asArray.fold(this)(f)
  final def withObject(f: JsonObject => Json): Json = asObject.fold(this)(f)

  def mapBoolean(f: Boolean => Boolean): Json
  def mapNumber(f: JsonNumber => JsonNumber): Json
  def mapString(f: String => String): Json
  def mapArray(f: Vector[Json] => Vector[Json]): Json
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
  final def as[A](implicit d: Decoder[A]): Decoder.Result[A] = d(hcursor)

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

  // Alias for `findAllByKey`.
  final def \\(key: String): List[Json] = findAllByKey(key)

  /**
   * Recursively return all values matching the specified `key`.
   *
   * The Play docs, from which this method was inspired, reads:
   *   "Lookup for fieldName in the current object and all descendants."
   */
  final def findAllByKey(key: String): List[Json] = keyValues(this).collect {
    case (k, v) if (k == key) => v
  }

  private def keyValues(json: Json): List[(String, Json)] = json match {
    case JObject(obj)  => obj.toList.flatMap { case (k, v) => keyValuesHelper(k, v) }
    case JArray(elems) => elems.toList.flatMap(keyValues)
    case _             => Nil
  }

  private def keyValuesHelper(key: String, value: Json): List[(String, Json)] =
    (key, value) :: keyValues(value)
}

final object Json {
  trait Folder[Z] extends Serializable {
    def onNull: Z
    def onBoolean(value: Boolean): Z
    def onNumber(value: JsonNumber): Z
    def onDouble(value: Double): Z
    def onFloat(value: Float): Z
    def onLong(value: Long): Z
    def onString(value: String): Z
    def onArray(value: Vector[Json]): Z
    def onObject(value: JsonObject): Z
  }

  private[circe] final case object JNull extends Json {
    final def foldWith[Z](folder: Folder[Z]): Z = folder.onNull

    final def isNull: Boolean = true
    final def isBoolean: Boolean = false
    final def isNumber: Boolean = false
    final def isString: Boolean = false
    final def isArray: Boolean = false
    final def isObject: Boolean = false

    final def asBoolean: Option[Boolean] = None
    final def asNumber: Option[JsonNumber] = None
    final def asString: Option[String] = None
    final def asArray: Option[Vector[Json]] = None
    final def asObject: Option[JsonObject] = None

    final def mapBoolean(f: Boolean => Boolean): Json = this
    final def mapNumber(f: JsonNumber => JsonNumber): Json = this
    final def mapString(f: String => String): Json = this
    final def mapArray(f: Vector[Json] => Vector[Json]): Json = this
    final def mapObject(f: JsonObject => JsonObject): Json = this
  }

  private[circe] final case class JBoolean(value: Boolean) extends Json {
    final def foldWith[Z](folder: Folder[Z]): Z = folder.onBoolean(value)

    final def isNull: Boolean = false
    final def isBoolean: Boolean = true
    final def isNumber: Boolean = false
    final def isString: Boolean = false
    final def isArray: Boolean = false
    final def isObject: Boolean = false

    final def asBoolean: Option[Boolean] = Some(value)
    final def asNumber: Option[JsonNumber] = None
    final def asString: Option[String] = None
    final def asArray: Option[Vector[Json]] = None
    final def asObject: Option[JsonObject] = None

    final def mapBoolean(f: Boolean => Boolean): Json = JBoolean(f(value))
    final def mapNumber(f: JsonNumber => JsonNumber): Json = this
    final def mapString(f: String => String): Json = this
    final def mapArray(f: Vector[Json] => Vector[Json]): Json = this
    final def mapObject(f: JsonObject => JsonObject): Json = this
  }

  private[circe] final case class JNumber(value: JsonNumber) extends Json {
    final def foldWith[Z](folder: Folder[Z]): Z = folder.onNumber(value)

    final def isNull: Boolean = false
    final def isBoolean: Boolean = false
    final def isNumber: Boolean = true
    final def isString: Boolean = false
    final def isArray: Boolean = false
    final def isObject: Boolean = false

    final def asBoolean: Option[Boolean] = None
    final def asNumber: Option[JsonNumber] = Some(value)
    final def asString: Option[String] = None
    final def asArray: Option[Vector[Json]] = None
    final def asObject: Option[JsonObject] = None

    final def mapBoolean(f: Boolean => Boolean): Json = this
    final def mapNumber(f: JsonNumber => JsonNumber): Json = JNumber(f(value))
    final def mapString(f: String => String): Json = this
    final def mapArray(f: Vector[Json] => Vector[Json]): Json = this
    final def mapObject(f: JsonObject => JsonObject): Json = this
  }

  private[circe] final case class JDouble(value: Double) extends Json {
    final def foldWith[Z](folder: Folder[Z]): Z = folder.onDouble(value)

    final def isNull: Boolean = false
    final def isBoolean: Boolean = false
    final def isNumber: Boolean = true
    final def isString: Boolean = false
    final def isArray: Boolean = false
    final def isObject: Boolean = false

    final def asBoolean: Option[Boolean] = None
    final def asNumber: Option[JsonNumber] = Some(JsonNumber.fromDouble(value))
    final def asString: Option[String] = None
    final def asArray: Option[Vector[Json]] = None
    final def asObject: Option[JsonObject] = None

    final def mapBoolean(f: Boolean => Boolean): Json = this
    final def mapNumber(f: JsonNumber => JsonNumber): Json = JNumber(f(JsonNumber.fromDouble(value)))
    final def mapString(f: String => String): Json = this
    final def mapArray(f: Vector[Json] => Vector[Json]): Json = this
    final def mapObject(f: JsonObject => JsonObject): Json = this
  }

  private[circe] final case class JFloat(value: Float) extends Json {
    final def foldWith[Z](folder: Folder[Z]): Z = folder.onFloat(value)

    final def isNull: Boolean = false
    final def isBoolean: Boolean = false
    final def isNumber: Boolean = true
    final def isString: Boolean = false
    final def isArray: Boolean = false
    final def isObject: Boolean = false

    final def asBoolean: Option[Boolean] = None
    final def asNumber: Option[JsonNumber] = Some(JsonNumber.fromFloat(value))
    final def asString: Option[String] = None
    final def asArray: Option[Vector[Json]] = None
    final def asObject: Option[JsonObject] = None

    final def mapBoolean(f: Boolean => Boolean): Json = this
    final def mapNumber(f: JsonNumber => JsonNumber): Json = JNumber(f(JsonNumber.fromFloat(value)))
    final def mapString(f: String => String): Json = this
    final def mapArray(f: Vector[Json] => Vector[Json]): Json = this
    final def mapObject(f: JsonObject => JsonObject): Json = this
  }

  private[circe] final case class JLong(value: Long) extends Json {
    final def foldWith[Z](folder: Folder[Z]): Z = folder.onLong(value)

    final def isNull: Boolean = false
    final def isBoolean: Boolean = false
    final def isNumber: Boolean = true
    final def isString: Boolean = false
    final def isArray: Boolean = false
    final def isObject: Boolean = false

    final def asBoolean: Option[Boolean] = None
    final def asNumber: Option[JsonNumber] = Some(JsonNumber.fromLong(value))
    final def asString: Option[String] = None
    final def asArray: Option[Vector[Json]] = None
    final def asObject: Option[JsonObject] = None

    final def mapBoolean(f: Boolean => Boolean): Json = this
    final def mapNumber(f: JsonNumber => JsonNumber): Json = JNumber(f(JsonNumber.fromLong(value)))
    final def mapString(f: String => String): Json = this
    final def mapArray(f: Vector[Json] => Vector[Json]): Json = this
    final def mapObject(f: JsonObject => JsonObject): Json = this
  }

  private[circe] final case class JString(value: String) extends Json {
    final def foldWith[Z](folder: Folder[Z]): Z = folder.onString(value)

    final def isNull: Boolean = false
    final def isBoolean: Boolean = false
    final def isNumber: Boolean = false
    final def isString: Boolean = true
    final def isArray: Boolean = false
    final def isObject: Boolean = false

    final def asBoolean: Option[Boolean] = None
    final def asNumber: Option[JsonNumber] = None
    final def asString: Option[String] = Some(value)
    final def asArray: Option[Vector[Json]] = None
    final def asObject: Option[JsonObject] = None

    final def mapBoolean(f: Boolean => Boolean): Json = this
    final def mapNumber(f: JsonNumber => JsonNumber): Json = this
    final def mapString(f: String => String): Json = JString(f(value))
    final def mapArray(f: Vector[Json] => Vector[Json]): Json = this
    final def mapObject(f: JsonObject => JsonObject): Json = this
  }

  private[circe] final case class JArray(value: Vector[Json]) extends Json {
    final def foldWith[Z](folder: Folder[Z]): Z = folder.onArray(value)

    final def isNull: Boolean = false
    final def isBoolean: Boolean = false
    final def isNumber: Boolean = false
    final def isString: Boolean = false
    final def isArray: Boolean = true
    final def isObject: Boolean = false

    final def asBoolean: Option[Boolean] = None
    final def asNumber: Option[JsonNumber] = None
    final def asString: Option[String] = None
    final def asArray: Option[Vector[Json]] = Some(value)
    final def asObject: Option[JsonObject] = None

    final def mapBoolean(f: Boolean => Boolean): Json = this
    final def mapNumber(f: JsonNumber => JsonNumber): Json = this
    final def mapString(f: String => String): Json = this
    final def mapArray(f: Vector[Json] => Vector[Json]): Json = JArray(f(value))
    final def mapObject(f: JsonObject => JsonObject): Json = this
  }

  private[circe] final case class JObject(value: JsonObject) extends Json {
    final def foldWith[Z](folder: Folder[Z]): Z = folder.onObject(value)

    final def isNull: Boolean = false
    final def isBoolean: Boolean = false
    final def isNumber: Boolean = false
    final def isString: Boolean = false
    final def isArray: Boolean = false
    final def isObject: Boolean = true

    final def asBoolean: Option[Boolean] = None
    final def asNumber: Option[JsonNumber] = None
    final def asString: Option[String] = None
    final def asArray: Option[Vector[Json]] = None
    final def asObject: Option[JsonObject] = Some(value)

    final def mapBoolean(f: Boolean => Boolean): Json = this
    final def mapNumber(f: JsonNumber => JsonNumber): Json = this
    final def mapString(f: String => String): Json = this
    final def mapArray(f: Vector[Json] => Vector[Json]): Json = this
    final def mapObject(f: JsonObject => JsonObject): Json = JObject(f(value))
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
  final def fromValues(values: Iterable[Json]): Json = JArray(values.toVector)

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
  final def fromInt(value: Int): Json = JLong(value.toLong)

  /**
   * Create a `Json` value representing a JSON number from a `Long`.
   */
  final def fromLong(value: Long): Json = JLong(value)

  /**
   * Try to create a `Json` value representing a JSON number from a `Double`.
   *
   * The result is empty if the argument cannot be represented as a JSON number.
   */
  final def fromDouble(value: Double): Option[Json] = if (isReal(value)) Some(JDouble(value)) else None

  /**
   * Try to create a `Json` value representing a JSON number from a `Float`.
   *
   * The result is empty if the argument cannot be represented as a JSON number.
   */
  final def fromFloat(value: Float): Option[Json] = if (isReal(value)) Some(JFloat(value)) else None

  /**
   * Create a `Json` value representing a JSON number or null from a `Double`.
   *
   * The result is a JSON null if the argument cannot be represented as a JSON
   * number.
   */
  final def fromDoubleOrNull(value: Double): Json = if (isReal(value)) JDouble(value) else Null

  /**
   * Create a `Json` value representing a JSON number or null from a `Float`.
   *
   * The result is a JSON null if the argument cannot be represented as a JSON
   * number.
   */
  final def fromFloatOrNull(value: Float): Json =
    if (isReal(value)) JFloat(value) else Null

  /**
   * Create a `Json` value representing a JSON number or string from a `Double`.
   *
   * The result is a JSON string if the argument cannot be represented as a JSON
   * number.
   */
  final def fromDoubleOrString(value: Double): Json =
    if (isReal(value)) JDouble(value) else fromString(java.lang.Double.toString(value))

  /**
   * Create a `Json` value representing a JSON number or string from a `Float`.
   *
   * The result is a JSON string if the argument cannot be represented as a JSON
   * number.
   */
  final def fromFloatOrString(value: Float): Json =
    if (isReal(value)) JFloat(value) else fromString(java.lang.Float.toString(value))

  /**
   * Create a `Json` value representing a JSON number from a `BigInt`.
   */
  final def fromBigInt(value: BigInt): Json = JNumber(JsonNumber.fromBigInteger(value.underlying))

  /**
   * Create a `Json` value representing a JSON number from a `BigDecimal`.
   */
  final def fromBigDecimal(value: BigDecimal): Json = JNumber(JsonNumber.fromBigDecimal(value.underlying))

  /**
   * Calling `.isNaN` and `.isInfinity` directly on the value boxes; we
   * explicitly avoid that here.
   */
  private[this] def isReal(value: Double): Boolean =
    (!java.lang.Double.isNaN(value)) && (!java.lang.Double.isInfinite(value))

  /**
   * Calling `.isNaN` and `.isInfinity` directly on the value boxes; we
   * explicitly avoid that here.
   */
  private[this] def isReal(value: Float): Boolean =
    (!java.lang.Float.isNaN(value)) && (!java.lang.Float.isInfinite(value))

  private[this] final def arrayEq(x: Seq[Json], y: Seq[Json]): Boolean = {
    val it0 = x.iterator
    val it1 = y.iterator
    while (it0.hasNext && it1.hasNext) {
      if (Json.eqJson.neqv(it0.next, it1.next)) return false
    }
    it0.hasNext == it1.hasNext
  }

  implicit final val eqJson: Eq[Json] = Eq.instance {
    case (          x,           y) if x.isNull && y.isNull => true
    case ( JObject(a),  JObject(b)) => JsonObject.eqJsonObject.eqv(a, b)
    case ( JString(a),  JString(b)) => a == b
    case (JBoolean(a), JBoolean(b)) => a == b
    case (  JArray(a),   JArray(b)) => arrayEq(a, b)
    case ( JNumber(a),  JNumber(b)) => a == b
    case ( JDouble(a),  JDouble(b)) => a == b
    case (  JFloat(a),   JFloat(b)) => a == b
    case (   JLong(a),    JLong(b)) => a == b
    case (          a,           b) if a.isNumber && b.isNumber => a.asNumber.get == b.asNumber.get
    case (          _,           _) => false
  }

  implicit final val showJson: Show[Json] = Show.fromToString[Json]
}
