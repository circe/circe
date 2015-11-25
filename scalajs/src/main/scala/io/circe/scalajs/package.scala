package io.circe

import cats.data.Xor
import io.circe.Json._
import scala.scalajs.js
import scala.scalajs.js.{ JavaScriptException, SyntaxError, undefined }
import scala.scalajs.js.JSConverters.{ JSRichGenMap, JSRichGenTraversableOnce }

package object scalajs {
  /**
   * Attempt to convert a value to [[Json]].
   */
  private[this] def unsafeConvertAnyToJson(input: Any): Json = input match {
    case s: String => Json.string(s)
    case n: Double => Json.numberOrNull(n)
    case true => Json.True
    case false => Json.False
    case null => Json.Empty
    case a: js.Array[_] => Json.fromValues(a.map(unsafeConvertAnyToJson(_: Any)))
    case o: js.Object => Json.fromFields(
      o.asInstanceOf[js.Dictionary[_]].mapValues(unsafeConvertAnyToJson).toSeq
    )
    case undefined => Json.Empty
  }

  /**
   * Convert [[scala.scalajs.js.Any]] to [[Json]].
   */
  def convertJsToJson(input: js.Any): Xor[Throwable, Json] = Xor.catchNonFatal(
    unsafeConvertAnyToJson(input)
  )

  /**
   * Decode [[scala.scalajs.js.Any]].
   */
  def decodeJs[A](input: js.Any)(implicit d: Decoder[A]): Xor[Throwable, A] =
    convertJsToJson(input).flatMap(d.decodeJson)

  /**
   * Convert [[Json]] to [[scala.scalajs.js.Any]].
   */
  def convertJsonToJs(input: Json): js.Any = input match {
    case JString(s) => s
    case JNumber(n) => n match {
      case JsonLong(x) => x
      case JsonDouble(x) => x
      case JsonDecimal(x) => x
      case JsonBigDecimal(x) => x.toDouble
    }
    case JBoolean(b) => b
    case JArray(arr) => arr.map(convertJsonToJs).toJSArray
    case JNull => undefined
    case JObject(obj) => obj.toMap.mapValues(convertJsonToJs).toJSDictionary
  }

  implicit class EncoderJsOps[A](val a: A) extends AnyVal {
    def asJsAny(implicit e: Encoder[A]): js.Any = convertJsonToJs(e(a))
  }

  implicit def decodeJsUndefOr[A](implicit d: Decoder[A]): Decoder[js.UndefOr[A]] =
    Decoder[Option[A]].map(_.fold[js.UndefOr[A]](js.undefined)(js.UndefOr.any2undefOrA))

  implicit def encodeJsUndefOr[A](implicit e: Encoder[A]): Encoder[js.UndefOr[A]] =
    Encoder.instance(_.fold(Json.empty)(e(_)))
}
