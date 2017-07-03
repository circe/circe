package io.circe.jawn

import io.circe.{ Json, JsonNumber, JsonObject }
import java.util.LinkedHashMap
import jawn.{ Facade, FContext, SupportParser }

final object CirceSupportParser extends SupportParser[Json] {
  implicit final val facade: Facade[Json] = new Facade[Json] {
    final def jnull(): Json = Json.Null
    final def jfalse(): Json = Json.False
    final def jtrue(): Json = Json.True
    final def jnum(s: CharSequence, decIndex: Int, expIndex: Int): Json =
      if (decIndex < 0 && expIndex < 0) {
        Json.fromJsonNumber(JsonNumber.fromIntegralStringUnsafe(s.toString))
      } else {
        Json.fromJsonNumber(JsonNumber.fromDecimalStringUnsafe(s.toString))
      }
    final def jstring(s: CharSequence): Json = Json.fromString(s.toString)

    final def singleContext(): FContext[Json] = new FContext[Json] {
      private[this] final var value: Json = null
      final def add(s: CharSequence): Unit = { value = jstring(s.toString) }
      final def add(v: Json): Unit =  { value = v }
      final def finish: Json = value
      final def isObj: Boolean = false
    }

    final def arrayContext(): FContext[Json] = new FContext[Json] {
      private[this] final val vs = Vector.newBuilder[Json]
      final def add(s: CharSequence): Unit = { vs += jstring(s.toString) }
      final def add(v: Json): Unit = { vs += v }
      final def finish: Json = Json.fromValues(vs.result())
      final def isObj: Boolean = false
    }

    def objectContext(): FContext[Json] = new FContext[Json] {
      private[this] final var key: String = null
      private[this] final val m = new LinkedHashMap[String, Json]

      final def add(s: CharSequence): Unit =
        if (key.eq(null)) { key = s.toString } else {
          m.put(key, jstring(s))
          key = null
        }
      final def add(v: Json): Unit = {
        m.put(key, v)
        key = null
      }
      final def finish: Json = Json.fromJsonObject(JsonObject.fromLinkedHashMap(m))
      final def isObj: Boolean = true
    }
  }
}
