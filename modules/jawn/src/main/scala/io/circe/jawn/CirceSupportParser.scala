package io.circe.jawn

import io.circe.{ Json, JsonNumber, JsonObject }
import java.io.Serializable
import java.util.LinkedHashMap
import org.typelevel.jawn.{ RawFacade, RawFContext, SupportParser }

class CirceSupportParser(maxValueSize: Option[Int], allowDuplicateKeys: Boolean)
    extends SupportParser[Json]
    with Serializable {
  implicit final val facade: RawFacade[Json] = maxValueSize match {
    case Some(size) =>
      if (allowDuplicateKeys) {
        new LimitedFacade(size) {
          protected[this] def mapPut(map: LinkedHashMap[String, Json], key: String, value: Json): Unit =
            map.put(key, value)
        }
      } else {
        new LimitedFacade(size) {
          protected[this] def mapPut(map: LinkedHashMap[String, Json], key: String, value: Json): Unit = {
            if (map.put(key, value).ne(null)) {
              throw new IllegalArgumentException(s"Invalid json, duplicate key name found: $key")
            }
          }
        }
      }
    case None =>
      if (allowDuplicateKeys) {
        new UnlimitedFacade {
          protected[this] def mapPut(map: LinkedHashMap[String, Json], key: String, value: Json): Unit =
            map.put(key, value)
        }
      } else {
        new UnlimitedFacade {
          protected[this] def mapPut(map: LinkedHashMap[String, Json], key: String, value: Json): Unit = {
            if (map.put(key, value).ne(null)) {
              throw new IllegalArgumentException(s"Invalid json, duplicate key name found: $key")
            }
          }
        }
      }
  }

  private[this] abstract class BaseFacade extends RawFacade[Json] with Serializable {
    final def jnull(index: Int): Json = Json.Null
    final def jfalse(index: Int): Json = Json.False
    final def jtrue(index: Int): Json = Json.True

    final def singleContext(index: Int): RawFContext[Json] = new RawFContext[Json] {
      private[this] final var value: Json = null
      final def add(s: CharSequence, index: Int): Unit = { value = jstring(s.toString, index) }
      final def add(v: Json, index: Int): Unit = { value = v }
      final def finish(index: Int): Json = value
      final def isObj: Boolean = false
    }

    final def arrayContext(index: Int): RawFContext[Json] = new RawFContext[Json] {
      private[this] final val vs = Vector.newBuilder[Json]
      final def add(s: CharSequence, index: Int): Unit = { vs += jstring(s.toString, index) }
      final def add(v: Json, index: Int): Unit = { vs += v }
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

    final def objectContext(index: Int): RawFContext[Json] = new RawFContext[Json] {
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

    final def objectContext(index: Int): RawFContext[Json] = new RawFContext[Json] {
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
}
