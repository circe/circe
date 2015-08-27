package io.circe

import algebra.Eq
import cats.Show
import cats.data.Xor
import cats.std.list._

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
  def fold[X](
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
  def arrayOrObject[X](
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
  def cursor: Cursor = Cursor(this)

  /**
   * Construct a cursor with history from this JSON value.
   */
  def hcursor: HCursor = Cursor(this).hcursor

  def isNull: Boolean = false
  def isBoolean: Boolean = false
  def isNumber: Boolean = false
  def isString: Boolean = false
  def isArray: Boolean = false
  def isObject: Boolean = false

  def asBoolean: Option[Boolean] = None
  def asNumber: Option[JsonNumber] = None
  def asString: Option[String] = None
  def asArray: Option[List[Json]] = None
  def asObject: Option[JsonObject] = None

  def withBoolean(f: Boolean => Json): Json = asBoolean.fold(this)(f)
  def withNumber(f: JsonNumber => Json): Json = asNumber.fold(this)(f)
  def withString(f: String => Json): Json = asString.fold(this)(f)
  def withArray(f: List[Json] => Json): Json = asArray.fold(this)(f)
  def withObject(f: JsonObject => Json): Json = asObject.fold(this)(f)

  def mapBoolean(f: Boolean => Boolean): Json = this
  def mapNumber(f: JsonNumber => JsonNumber): Json = this
  def mapString(f: String => String): Json = this
  def mapArray(f: List[Json] => List[Json]): Json = this
  def mapObject(f: JsonObject => JsonObject): Json = this

  /**
   * The name of the type of the JSON value.
   */
  def name: String =
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
  def as[A](implicit d: Decoder[A]): Decoder.Result[A] = d(cursor.hcursor)

  /**
   * Pretty-print this JSON value to a string using the given pretty-printer.
   */
  def pretty(p: Printer): String = p.pretty(this)

  /**
   * Pretty-print this JSON value to a string with no spaces.
   */
  def noSpaces: String = Printer.noSpaces.pretty(this)

  /**
   * Pretty-print this JSON value to a string indentation of two spaces.
   */
  def spaces2: String = Printer.spaces2.pretty(this)

  /**
   * Pretty-print this JSON value to a string indentation of four spaces.
   */
  def spaces4: String = Printer.spaces4.pretty(this)

  /**
   * Type-safe equality method.
   */
  def ===(that: Json): Boolean = {
    def arrayEq(x: Seq[Json], y: Seq[Json]): Boolean = {
      val it0 = x.iterator
      val it1 = y.iterator
      while (it0.hasNext && it1.hasNext) {
        if (it0.next =!= it1.next) return false
      }
      it0.hasNext == it1.hasNext
    }

    (this, that) match {
      case ( JObject(a),  JObject(b)) => a === b
      case ( JString(a),  JString(b)) => a == b
      case ( JNumber(a),  JNumber(b)) => a === b
      case (JBoolean(a), JBoolean(b)) => a == b
      case (  JArray(a),   JArray(b)) => arrayEq(a, b)
      case (          x,           y) => x.isNull && y.isNull
    }
  }

  /**
   * Type-safe inequality.
   */
  def =!=(that: Json): Boolean = !(this === that)

  /**
   * Compute a `String` representation for this JSON value.
   */
  override def toString: String = spaces2

  /**
   * Universal equality derived from our type-safe equality.
   */
  override def equals(that: Any): Boolean =
    that match {
      case j: Json => this === j
      case _ => false
    }

  /**
   * Hashing that is consistent with our universal equality.
   */
  override def hashCode: Int = super.hashCode
}

object Json {
  private[circe] case object JNull extends Json {
    override def isNull: Boolean = true
  }
  private[circe] final case class JBoolean(b: Boolean) extends Json {
    override def isBoolean: Boolean = true
    override def asBoolean: Option[Boolean] = Some(b)
    override def mapBoolean(f: Boolean => Boolean): Json = JBoolean(f(b))
  }
  private[circe] final case class JNumber(n: JsonNumber) extends Json {
    override def isNumber: Boolean = true
    override def asNumber: Option[JsonNumber] = Some(n)
    override def mapNumber(f: JsonNumber => JsonNumber): Json = JNumber(f(n))
  }
  private[circe] final case class JString(s: String) extends Json {
    override def isString: Boolean = true
    override def asString: Option[String] = Some(s)
    override def mapString(f: String => String): Json = JString(f(s))
  }
  private[circe] final case class JArray(a: Seq[Json]) extends Json {
    override def isArray: Boolean = true
    override def asArray: Option[List[Json]] = Some(a.toList)
    override def mapArray(f: List[Json] => List[Json]): Json = JArray(f(a.toList))
  }
  private[circe] final case class JObject(o: JsonObject) extends Json {
    override def isObject: Boolean = true
    override def asObject: Option[JsonObject] = Some(o)
    override def mapObject(f: JsonObject => JsonObject): Json = JObject(f(o))
  }

  def empty: Json = Empty

  val Empty: Json = JNull
  val True: Json = JBoolean(true)
  val False: Json = JBoolean(false)

  def bool(b: Boolean): Json = JBoolean(b)
  def int(n: Int): Json = JNumber(JsonLong(n.toLong))
  def long(n: Long): Json = JNumber(JsonLong(n))
  def number(n: Double): Option[Json] = JsonDouble(n).asJson
  def bigDecimal(n: BigDecimal): Json = JNumber(JsonBigDecimal(n))
  def numberOrNull(n: Double): Json = JsonDouble(n).asJsonOrNull
  def numberOrString(n: Double): Json = JsonDouble(n).asJsonOrString
  def string(s: String): Json = JString(s)
  def array(elements: Json*): Json = JArray(elements)
  def obj(fields: (String, Json)*): Json = JObject(JsonObject.from(fields.toList))

  def fromJsonNumber(num: JsonNumber): Json = JNumber(num)
  def fromJsonObject(obj: JsonObject): Json = JObject(obj)
  def fromFields(fields: Seq[(String, Json)]): Json = JObject(JsonObject.from(fields.toList))
  def fromValues(values: Seq[Json]): Json = JArray(values)

  implicit val eqJson: Eq[Json] = Eq.instance(_ === _)
  implicit val showJson: Show[Json] = Show.fromToString[Json]
}
