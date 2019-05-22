package io.circe.jawn

import io.circe.{ Json, JsonNumber, JsonObject }
import java.io.Serializable
import java.util.LinkedHashMap
import org.typelevel.jawn.{ RawFacade, RawFContext, SupportParser }

class CirceSupportParser(maxValueSize: Option[Int], allowDuplicateKeys: Boolean) extends SupportParser[Json] with Serializable {
  implicit final val facade: RawFacade[Json] = maxValueSize match {
    case Some(size) => new LimitedFacade(size, allowDuplicateKeys)
    case None       => new UnlimitedFacade(allowDuplicateKeys)
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
  }

  private[this] final class LimitedFacade(maxValueSize: Int, allowDuplicateKeys: Boolean) extends BaseFacade {
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
          safePut(key, jstring(s, index), m, allowDuplicateKeys)
          key = null
        }
      final def add(v: Json, index: Int): Unit = {
        safePut(key, v, m, allowDuplicateKeys)
        key = null
      }
      final def finish(index: Int): Json = Json.fromJsonObject(JsonObject.fromLinkedHashMap(m))
      final def isObj: Boolean = true
    }
  }

  private[this] final class UnlimitedFacade(allowDuplicateKeys: Boolean) extends BaseFacade {
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
          safePut(key, jstring(s, index), m, allowDuplicateKeys)
          key = null
        }
      final def add(v: Json, index: Int): Unit = {
        safePut(key, v, m, allowDuplicateKeys)
        key = null
      }
      final def finish(index: Int): Json = Json.fromJsonObject(JsonObject.fromLinkedHashMap(m))
      final def isObj: Boolean = true
    }
  }

  private def safePut[K, V](key: K, value: V, map: LinkedHashMap[K, V], allowDuplicateKeys: Boolean): V = {
    val oldValue = map.put(key, value)
    if (oldValue != null && !allowDuplicateKeys)
      throw new IllegalArgumentException(s"Invalid json, duplicate key name found: $key")
    else oldValue
  }


}
