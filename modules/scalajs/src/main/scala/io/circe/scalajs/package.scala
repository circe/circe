package io.circe

import io.circe.Json._
import scala.scalajs.js
import scala.scalajs.js.undefined
import scala.scalajs.js.JSConverters.{ JSRichGenMap, JSRichGenTraversableOnce }
import scala.util.control.NonFatal

package object scalajs {
  /**
   * Attempt to convert a value to [[Json]].
   */
  private[this] def unsafeConvertAnyToJson(input: Any): Json = input match {
    case s: String => Json.fromString(s)
    case n: Double => Json.fromDoubleOrNull(n)
    case true => Json.True
    case false => Json.False
    case null => Json.Null
    case a: js.Array[_] => Json.fromValues(a.map(unsafeConvertAnyToJson(_: Any)))
    case o: js.Object => Json.fromFields(
      o.asInstanceOf[js.Dictionary[_]].mapValues(unsafeConvertAnyToJson).toSeq
    )
    case other if js.isUndefined(other) => Json.Null
  }

  /**
   * Convert [[scala.scalajs.js.Any]] to [[Json]].
   */
  final def convertJsToJson(input: js.Any): Either[Throwable, Json] =
    try Right(unsafeConvertAnyToJson(input)) catch {
      case NonFatal(exception) => Left(exception)
    }

  /**
   * Decode [[scala.scalajs.js.Any]].
   */
  final def decodeJs[A](input: js.Any)(implicit d: Decoder[A]): Either[Throwable, A] =
    convertJsToJson(input) match {
      case Right(json) => d.decodeJson(json)
      case l @ Left(_) => l.asInstanceOf[Either[Throwable, A]]
    }

  /**
   * Convert [[Json]] to [[scala.scalajs.js.Any]].
   */
  final def convertJsonToJs(input: Json): js.Any = input match {
    case JString(s) => s
    case JNumber(n) => n.toDouble
    case JBoolean(b) => b
    case JArray(arr) => arr.map(convertJsonToJs).toJSArray
    case JNull => null
    case JObject(obj) => obj.toMap.mapValues(convertJsonToJs).toJSDictionary
  }

  implicit final class EncoderJsOps[A](val wrappedEncodeable: A) extends AnyVal {
    def asJsAny(implicit encoder: Encoder[A]): js.Any = convertJsonToJs(encoder(wrappedEncodeable))
  }

  implicit final def decodeJsUndefOr[A](implicit d: Decoder[A]): Decoder[js.UndefOr[A]] =
    Decoder[Option[A]].map(_.fold[js.UndefOr[A]](js.undefined)(js.UndefOr.any2undefOrA))

  implicit final def encodeJsUndefOr[A](implicit e: Encoder[A]): Encoder[js.UndefOr[A]] =
    Encoder.instance(_.fold(Json.Null)(e(_)))
}
