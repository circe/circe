package io.circe.jawn

import io.circe.{ Json, JsonNumber, JsonObject }
import java.util.LinkedHashMap
import jawn.{ RawFacade, RawFContext, SupportParser }

final object CirceSupportParser extends SupportParser[Json] {
  implicit final val facade: RawFacade[Json] = new RawFacade[Json] {
    final def jnull(index: Int): Json = Json.Null
    final def jfalse(index: Int): Json = Json.False
    final def jtrue(index: Int): Json = Json.True
    final def jnum(s: CharSequence, decIndex: Int, expIndex: Int, index: Int): Json =
      if (decIndex < 0 && expIndex < 0) {
        Json.fromJsonNumber(JsonNumber.fromIntegralStringUnsafe(s.toString))
      } else {
        Json.fromJsonNumber(JsonNumber.fromDecimalStringUnsafe(s.toString))
      }
    final def jstring(s: CharSequence, index: Int): Json = Json.fromString(s.toString)

    final def singleContext(index: Int): RawFContext[Json] = new RawFContext[Json] {
      private[this] final var value: Json = null
      final def add(s: CharSequence, index: Int): Unit = { value = jstring(s.toString, index) }
      final def add(v: Json, index: Int): Unit =  { value = v }
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

    def objectContext(index: Int): RawFContext[Json] = new RawFContext[Json] {
      private[this] final var key: String = null
      private[this] final val m = new LinkedHashMap[String, Json]

      final def add(s: CharSequence, index: Int): Unit =
        if (key.eq(null)) { key = s.toString } else {
          m.put(key, jstring(s, index))
          key = null
        }
      final def add(v: Json, index: Int): Unit = {
        m.put(key, v)
        key = null
      }
      final def finish(index: Int): Json = Json.fromJsonObject(JsonObject.fromLinkedHashMap(m))
      final def isObj: Boolean = true
    }
  }
}
