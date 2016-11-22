package io.circe.jawn

import io.circe.ast.{ Json, JsonNumber, JsonObject }
import jawn.{ Facade, FContext, SupportParser }

final object CirceSupportParser extends SupportParser[Json] {
  implicit final val facade: Facade[Json] = new Facade[Json] {
    final def jnull(): Json = Json.Null
    final def jfalse(): Json = Json.False
    final def jtrue(): Json = Json.True
    final def jnum(s: String): Json = Json.JNumber(JsonNumber.fromDecimalStringUnsafe(s))
    final def jint(s: String): Json = Json.JNumber(JsonNumber.fromIntegralStringUnsafe(s))
    final def jstring(s: String): Json = Json.fromString(s)

    final def singleContext(): FContext[Json] = new FContext[Json] {
      private[this] final var value: Json = null
      final def add(s: String): Unit = { value = jstring(s) }
      final def add(v: Json): Unit =  { value = v }
      final def finish: Json = value
      final def isObj: Boolean = false
    }

    final def arrayContext(): FContext[Json] = new FContext[Json] {
      private[this] final val vs = List.newBuilder[Json]
      final def add(s: String): Unit = { vs += jstring(s) }
      final def add(v: Json): Unit = { vs += v }
      final def finish: Json = Json.fromValues(vs.result())
      final def isObj: Boolean = false
    }

    def objectContext(): FContext[Json] = new FContext[Json] {
      private[this] final var key: String = null
      private[this] final val m = scala.collection.mutable.Map.empty[String, Json]
      private[this] final val keys = Vector.newBuilder[String]

      final def add(s: String): Unit =
        if (key == null) { key = s } else {
          if (!m.contains(key)) keys += key
          m(key) = jstring(s)
          key = null
        }
      final def add(v: Json): Unit = {
        if (!m.contains(key)) keys += key
        m(key) = v
        key = null
      }
      final def finish: Json = Json.fromJsonObject(JsonObject.fromMapAndVector(m.toMap, keys.result()))
      final def isObj: Boolean = true
    }
  }
}
