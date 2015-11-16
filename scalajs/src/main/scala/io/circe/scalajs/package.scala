package io.circe

import cats.data.Xor
import io.circe.Decoder.Result
import io.circe.Json._
import scala.scalajs.js
import scala.scalajs.js.JSConverters.{JSRichGenMap, JSRichGenTraversableOnce}
import scala.scalajs.js.{SyntaxError, JavaScriptException, undefined}

package object scalajs {

  /**
   * Converts scalajs js.Any to circe Json
   */
  def parseJS(input: js.Any): Xor[ParsingFailure, Json] = try {
    Xor.right(convertJSAnyToJson(input))
  } catch {
    case exception@JavaScriptException(error: SyntaxError) =>
      Xor.left(ParsingFailure(error.message, exception))
  }

  /**
   * converts scalajs objects to scala classes
   */
  def decodeJS[A](input: js.Any)(implicit d: Decoder[A]): Xor[Error, A] =
    parseJS(input).flatMap(d.decodeJson)

  /**
   * convert circe Json to scalajs js.Any
   */
  def convertJsonToJSAny(input: Json): js.Any = {
    input match {
      case JString(s) => s
      case JNumber(n) => n match {
        case JsonLong(x) => x
        case JsonDouble(x) => x
        case JsonDecimal(x) => x
        case JsonBigDecimal(x) => x.toDouble
      }
      case JBoolean(b) => b
      case JArray(arr) => arr.map(convertJsonToJSAny).toJSArray
      case JNull => undefined
      case JObject(o) => o.toMap.mapValues(convertJsonToJSAny).toJSDictionary
    }
  }

  /**
   * converts scalajs js.Any to circe Json
   */
  def convertJSAnyToJson(j: Any): Json = j match {
    case s: String => Json.string(s)
    case n: Double => Json.numberOrNull(n)
    case true => Json.True
    case false => Json.False
    case null => Json.Empty
    case a: js.Array[_] => Json.fromValues(a.map(convertJSAnyToJson(_: Any)))
    case o: js.Object => Json.fromFields(
      o.asInstanceOf[js.Dictionary[_]].mapValues(convertJSAnyToJson).toSeq
    )
    case undefined => Json.Empty
  }


  implicit class EncoderJSOps[A](val a: A) extends AnyVal {
    def asJSAny(implicit e: Encoder[A]): js.Any = convertJsonToJSAny(e(a))
  }

  /**
   * @group Decoding
   */
  implicit def decodeJSUndefined[A](implicit d: Decoder[A]): Decoder[js.UndefOr[A]] =
    Decoder.withReattempt { a =>
      a.success.fold[Result[js.UndefOr[A]]](Xor.right(js.undefined)) { valid =>
        if (valid.focus.isNull) Xor.right(js.undefined)
        else d(valid).fold[Result[js.UndefOr[A]]](
          df =>
            df.history.headOption.fold[Result[js.UndefOr[A]]](
              Xor.right(js.undefined)
            )(_ => Xor.left(df)),
          a => Xor.right(a)
        )
      }
    }


  /**
   * @group Encoding
   */
  implicit def encodeJSUndefined[A](implicit e: Encoder[A]): Encoder[js.UndefOr[A]] =
    Encoder.instance(_.fold(Json.empty)(e(_)))

}