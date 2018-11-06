package io.circe

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.util.control.NonFatal

package object scalajs {

  /**
   * Attempt to convert a value to [[Json]].
   */
  private[this] def convertAnyToJsonUnsafe(input: Any): Json = input match {
    case s: String      => Json.fromString(s)
    case n: Double      => Json.fromDoubleOrNull(n)
    case true           => Json.True
    case false          => Json.False
    case null           => Json.Null
    case a: js.Array[_] => Json.fromValues(a.map(convertAnyToJsonUnsafe(_: Any)))
    case o: js.Object =>
      Json.fromFields(
        o.asInstanceOf[js.Dictionary[_]].mapValues(convertAnyToJsonUnsafe).toSeq
      )
    case other if js.isUndefined(other) => Json.Null
  }

  /**
   * Convert [[scala.scalajs.js.Any]] to [[Json]].
   */
  final def convertJsToJson(input: js.Any): Either[Throwable, Json] =
    try Right(convertAnyToJsonUnsafe(input))
    catch {
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

  private[this] val toJsAnyFolder: Json.Folder[js.Any] = new Json.Folder[js.Any] with Function1[Json, js.Any] {
    def apply(value: Json): js.Any = value.foldWith(this)

    def onNull: js.Any = null
    def onBoolean(value: Boolean): js.Any = value
    def onNumber(value: JsonNumber): js.Any = value.toDouble
    def onString(value: String): js.Any = value
    def onArray(value: Vector[Json]): js.Any = value.map(this).toJSArray
    def onObject(value: JsonObject): js.Any = value.toMap.mapValues(this).toJSDictionary
  }

  /**
   * Convert [[Json]] to [[scala.scalajs.js.Any]].
   */
  final def convertJsonToJs(input: Json): js.Any = input.foldWith(toJsAnyFolder)

  implicit final class EncoderJsOps[A](val wrappedEncodeable: A) extends AnyVal {
    def asJsAny(implicit encoder: Encoder[A]): js.Any = convertJsonToJs(encoder(wrappedEncodeable))
  }

  implicit final def decodeJsUndefOr[A](implicit d: Decoder[A]): Decoder[js.UndefOr[A]] =
    Decoder[Option[A]].map(_.fold[js.UndefOr[A]](js.undefined)(js.UndefOr.any2undefOrA))

  implicit final def encodeJsUndefOr[A](implicit e: Encoder[A]): Encoder[js.UndefOr[A]] =
    Encoder.instance(_.fold(Json.Null)(e(_)))
}
