package io.jfc

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
    jsonBool: Boolean => X,
    jsonNumber: JsonNumber => X,
    jsonString: String => X,
    jsonArray: List[Json] => X,
    jsonObject: JsonObject => X
  ): X = this match {
    case JNull      => jsonNull
    case JBool(b)   => jsonBool(b)
    case JNumber(n) => jsonNumber(n)
    case JString(s) => jsonString(s)
    case JArray(a)  => jsonArray(a.toList)
    case JObject(o) => jsonObject(o)
  }

  /**
   * Run on an array or object or return the given default.
   */
  def arrayOrObject[X](
    or: => X,
    jsonArray: List[Json] => X,
    jsonObject: JsonObject => X
  ): X = this match {
    case JNull      => or
    case JBool(_)   => or
    case JNumber(_) => or
    case JString(_) => or
    case JArray(a)  => jsonArray(a.toList)
    case JObject(o) => jsonObject(o)
  }

  /**
   * Construct a cursor from this JSON value.
   */
  def cursor: Cursor = Cursor(this)

  /**
   * Returns the possible object of this JSON value.
   */
  def asObj: Option[JsonObject] = None

  def asArray: Option[List[Json]] = None

  def isNull: Boolean = false
  def isArray: Boolean = false

  /**
   * The name of the type of the JSON value.
   */
  def name: String =
    this match {
      case JNull      => "Null"
      case JBool(_)   => "Boolean"
      case JNumber(_) => "Number"
      case JString(_) => "String"
      case JArray(_)  => "Array"
      case JObject(_) => "Object"
    }

  /**
   * Attempts to decode this JSON value to another data type.
   */
  def as[A](implicit d: Decode[A]): Xor[DecodeFailure, A] = d(cursor.hcursor)

  /**
   * Pretty-print this JSON value to a string using the given pretty-printing parameters.
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
   * Compute a `String` representation for this JSON value.
   */
  override def toString: String = spaces2
}

object Json {
  private[jfc] case object JNull extends Json {
    override def isNull: Boolean = true
  }
  private[jfc] final case class JBool(b: Boolean) extends Json
  private[jfc] final case class JString(s: String) extends Json
  private[jfc] final case class JArray(a: Seq[Json]) extends Json {
    override def isArray: Boolean = true
    override def asArray: Option[List[Json]] = Some(a.toList)
  }
  private[jfc] final case class JObject(o: JsonObject) extends Json {
    override def asObj: Option[JsonObject] = Some(o)
  }
  private[jfc] final case class JNumber(n: JsonNumber) extends Json

  val empty: Json = JNull
  def bool(b: Boolean): Json = JBool(b)
  def int(n: Int): Json = JNumber(JsonLong(n.toLong))
  def long(n: Long): Json = JNumber(JsonLong(n))
  def number(n: Double): Option[Json] = JsonDouble(n).asJson
  def numberOrNull(n: Double): Json = JsonDouble(n).asJsonOrNull
  def numberOrString(n: Double): Json = JsonDouble(n).asJsonOrString
  def string(s: String): Json = JString(s)
  def array(elements: Json*): Json = JArray(elements)
  def obj(fields: (String, Json)*): Json = JObject(JsonObject.from(fields.toList))

  def fromJsonNumber(num: JsonNumber): Json = JNumber(num)
  def fromJsonObject(obj: JsonObject): Json = JObject(obj)
  def fromFields(fields: Seq[(String, Json)]): Json = JObject(JsonObject.from(fields.toList))
  def fromValues(values: Seq[Json]): Json = JArray(values)

  implicit val eqJson: Eq[Json] = Eq.instance {
    case (JObject(a), JObject(b)) => Eq[JsonObject].eqv(a, b)
    case (JString(a), JString(b)) => a == b
    case (JNumber(a), JNumber(b)) => Eq[JsonNumber].eqv(a, b)
    case (  JBool(a),   JBool(b)) => a == b
    case ( JArray(a),  JArray(b)) => Eq[List[Json]].eqv(a.toList, b.toList)
    case (     JNull,      JNull) => true
    case (         _,          _) => false
  }

  implicit val showJson: Show[Json] = Show.fromToString[Json]
}
