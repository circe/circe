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

package io.circe.jawn

import io.circe.Json
import io.circe.JsonNumber
import io.circe.JsonObject
import org.typelevel.jawn.FContext
import org.typelevel.jawn.Facade
import org.typelevel.jawn.SupportParser

import java.io.Serializable
import java.util.LinkedHashMap

object CirceSupportParser extends CirceSupportParser(None, true)

class CirceSupportParser(maxValueSize: Option[Int], allowDuplicateKeys: Boolean)
    extends SupportParser[Json]
    with Serializable {
  implicit final val facade: Facade[Json] = maxValueSize match {
    case Some(size) =>
      if (allowDuplicateKeys) {
        new LimitedFacade(size) with DuplicatesFacade
      } else {
        new LimitedFacade(size) with NoDuplicatesFacade
      }
    case None =>
      if (allowDuplicateKeys) {
        new UnlimitedFacade with DuplicatesFacade
      } else {
        new UnlimitedFacade with NoDuplicatesFacade
      }
  }

  private[this] abstract class BaseFacade extends Facade[Json] with Serializable {
    final def jnull(index: Int): Json = Json.Null
    final def jfalse(index: Int): Json = Json.False
    final def jtrue(index: Int): Json = Json.True

    final def singleContext(index: Int): FContext[Json] = new FContext[Json] {
      private[this] final var value: Json = null
      final def add(s: CharSequence, index: Int): Unit = value = jstring(s.toString, index)
      final def add(v: Json, index: Int): Unit = value = v
      final def finish(index: Int): Json = value
      final def isObj: Boolean = false
    }

    final def arrayContext(index: Int): FContext[Json] = new FContext[Json] {
      private[this] final val vs = Vector.newBuilder[Json]
      final def add(s: CharSequence, index: Int): Unit = vs += jstring(s.toString, index)
      final def add(v: Json, index: Int): Unit = vs += v
      final def finish(index: Int): Json = Json.fromValues(vs.result())
      final def isObj: Boolean = false
    }

    protected[this] def mapPut(map: LinkedHashMap[String, Json], key: String, value: Json): Unit
  }

  private[this] abstract class LimitedFacade(maxValueSize: Int) extends BaseFacade {
    final def jnum(s: CharSequence, decIndex: Int, expIndex: Int, index: Int): Json =
      if (s.length > maxValueSize) {
        throw new IllegalArgumentException(s"JSON number length (${s.length}) exceeds limit ($maxValueSize)")
      } else {
        if (decIndex < 0 && expIndex < 0) {
          Json.fromJsonNumber(JsonNumber.fromIntegralStringUnsafe(s.toString))
        } else {
          Json.fromJsonNumber(JsonNumber.fromDecimalStringUnsafe(s.toString))
        }
      }
    final def jstring(s: CharSequence, index: Int): Json = if (s.length > maxValueSize) {
      throw new IllegalArgumentException(s"JSON string length (${s.length}) exceeds limit ($maxValueSize)")
    } else {
      Json.fromString(s.toString)
    }

    final def objectContext(index: Int): FContext[Json] = new FContext[Json] {
      private[this] final var key: String = null
      private[this] final val m = new LinkedHashMap[String, Json]

      final def add(s: CharSequence, index: Int): Unit =
        if (key.eq(null)) {
          if (s.length > maxValueSize) {
            throw new IllegalArgumentException(s"JSON key length (${s.length}) exceeds limit ($maxValueSize)")
          } else {
            key = s.toString
          }
        } else {
          mapPut(m, key, jstring(s, index))
          key = null
        }
      final def add(v: Json, index: Int): Unit = {
        mapPut(m, key, v)
        key = null
      }
      final def finish(index: Int): Json = Json.fromJsonObject(JsonObject.fromLinkedHashMap(m))
      final def isObj: Boolean = true
    }
  }

  private[this] abstract class UnlimitedFacade extends BaseFacade {
    final def jnum(s: CharSequence, decIndex: Int, expIndex: Int, index: Int): Json =
      if (decIndex < 0 && expIndex < 0) {
        Json.fromJsonNumber(JsonNumber.fromIntegralStringUnsafe(s.toString))
      } else {
        Json.fromJsonNumber(JsonNumber.fromDecimalStringUnsafe(s.toString))
      }
    final def jstring(s: CharSequence, index: Int): Json = Json.fromString(s.toString)

    final def objectContext(index: Int): FContext[Json] = new FContext[Json] {
      private[this] final var key: String = null
      private[this] final val m = new LinkedHashMap[String, Json]

      final def add(s: CharSequence, index: Int): Unit =
        if (key.eq(null)) {
          key = s.toString
        } else {
          mapPut(m, key, jstring(s, index))
          key = null
        }
      final def add(v: Json, index: Int): Unit = {
        mapPut(m, key, v)
        key = null
      }
      final def finish(index: Int): Json = Json.fromJsonObject(JsonObject.fromLinkedHashMap(m))
      final def isObj: Boolean = true
    }
  }

  private[this] trait DuplicatesFacade extends BaseFacade {
    protected[this] final def mapPut(map: LinkedHashMap[String, Json], key: String, value: Json): Unit = {
      map.put(key, value)
      ()
    }
  }

  private[this] trait NoDuplicatesFacade extends BaseFacade {
    protected[this] final def mapPut(map: LinkedHashMap[String, Json], key: String, value: Json): Unit =
      if (map.put(key, value).ne(null)) {
        throw new IllegalArgumentException(s"Invalid json, duplicate key name found: $key")
      }
  }
}
