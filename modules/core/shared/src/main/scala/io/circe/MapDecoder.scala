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

import cats.data.NonEmptyList
import cats.data.Validated
import io.circe.DecodingFailure.Reason.WrongTypeExpectation
import io.circe.cursor.ObjectCursor

import scala.collection.Map
import scala.collection.mutable.Builder

private[circe] abstract class MapDecoder[K, V, M[K, V] <: Map[K, V]](
  decodeK: KeyDecoder[K],
  decodeV: Decoder[V]
) extends Decoder[M[K, V]] {
  private[this] val alwaysDecodeK = decodeK match {
    case decodeK: KeyDecoder.AlwaysKeyDecoder[K] => decodeK
    case _                                       => null
  }

  protected def createBuilder(): Builder[(K, V), M[K, V]]

  final def apply(c: HCursor): Decoder.Result[M[K, V]] = c.value match {
    case Json.JObject(obj) => decodeJsonObject(c, obj)
    case json              => MapDecoder.notJsObjectFailureResult[M[K, V]](c, json)
  }

  private[this] final def createObjectCursor(c: HCursor, obj: JsonObject, key: String): HCursor =
    new ObjectCursor(obj, key, c, false)(c, CursorOp.DownField(key))

  private[this] final def handleResult(key: K, c: HCursor, builder: Builder[(K, V), M[K, V]]): DecodingFailure =
    decodeV(c) match {
      case Right(value) => builder += ((key, value)); null
      case Left(error)  => error
    }

  private[this] final def decodeJsonObject(c: HCursor, obj: JsonObject): Decoder.Result[M[K, V]] = {
    val it = obj.keys.iterator
    val builder = createBuilder()
    var failed: DecodingFailure = null

    while (failed.eq(null) && it.hasNext) {
      val key = it.next()
      val atH = createObjectCursor(c, obj, key)

      failed = if (alwaysDecodeK.ne(null)) {
        handleResult(alwaysDecodeK.decodeSafe(key), atH, builder)
      } else {
        decodeK(key) match {
          case None    => MapDecoder.invalidKeyfailure(atH)
          case Some(k) => handleResult(k, atH, builder)
        }
      }
    }

    if (failed.eq(null)) Right(builder.result()) else Left(failed)
  }

  final override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[M[K, V]] = c.value match {
    case Json.JObject(obj) =>
      val it = obj.keys.iterator
      val builder = createBuilder()
      var failed = false
      val failures = List.newBuilder[DecodingFailure]

      while (it.hasNext) {
        val key = it.next()
        val atH = createObjectCursor(c, obj, key)

        if (alwaysDecodeK.ne(null)) {
          decodeV.decodeAccumulating(atH) match {
            case Validated.Valid(value) => if (!failed) builder += ((alwaysDecodeK.decodeSafe(key), value))
            case Validated.Invalid(es)  =>
              failed = true
              failures += es.head
              failures ++= es.tail
          }
        } else {
          decodeK(key) match {
            case Some(k) =>
              decodeV.decodeAccumulating(atH) match {
                case Validated.Valid(value) => if (!failed) builder += ((k, value))
                case Validated.Invalid(es)  =>
                  failed = true
                  failures += es.head
                  failures ++= es.tail
              }
            case None =>
              failed = true
              failures += MapDecoder.invalidKeyfailure(atH)
          }
        }
      }

      if (!failed) Validated.valid(builder.result())
      else {
        failures.result() match {
          case h :: t => Validated.invalid(NonEmptyList(h, t))
          case Nil    => Validated.valid(builder.result())
        }
      }
    case json => MapDecoder.notJsObjectfailureAccumulatingResult[M[K, V]](c, json)
  }
}

private[circe] object MapDecoder {
  @deprecated("use invalidKeyFailure", since = "0.14.2")
  final def failure(c: HCursor): DecodingFailure = DecodingFailure("[K, V]Map[K, V]", c.history)
  @deprecated("use invalidKeyFailure", since = "0.14.2")
  final def failureResult[A](c: HCursor): Decoder.Result[A] = Left[DecodingFailure, A](failure(c))
  @deprecated("use invalidKeyFailure", since = "0.14.2")
  final def failureAccumulatingResult[A](c: HCursor): Decoder.AccumulatingResult[A] =
    Validated.invalidNel[DecodingFailure, A](failure(c))

  final def invalidKeyfailure(c: HCursor): DecodingFailure =
    DecodingFailure("Couldn't decode key.", c.history)
  final def notJsObjectFailure(c: HCursor, value: Json): DecodingFailure =
    DecodingFailure(WrongTypeExpectation("object", value), c.history)
  final def notJsObjectFailureResult[A](c: HCursor, value: Json): Decoder.Result[A] =
    Left[DecodingFailure, A](notJsObjectFailure(c, value))
  final def notJsObjectfailureAccumulatingResult[A](c: HCursor, value: Json): Decoder.AccumulatingResult[A] =
    Validated.invalidNel[DecodingFailure, A](notJsObjectFailure(c, value))
}
