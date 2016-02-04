package io.circe.jawn

import io.circe.{ Json, JsonNumber }
import jawn.{ Facade, FContext, SupportParser }
import scala.collection.mutable.ArrayBuffer

final object CirceSupportParser extends SupportParser[Json] {
  implicit final val facade: Facade[Json] = new Facade[Json] {
    final def jnull(): Json = Json.Empty
    final def jfalse(): Json = Json.False
    final def jtrue(): Json = Json.True
    final def jnum(s: String): Json = Json.JNumber(JsonNumber.unsafeDecimal(s))
    final def jint(s: String): Json = Json.JNumber(JsonNumber.unsafeIntegral(s))
    final def jstring(s: String): Json = Json.JString(s)

    final def singleContext(): FContext[Json] = new FContext[Json] {
      private[this] final var value: Json = null
      final def add(s: String): Unit = { value = jstring(s) }
      final def add(v: Json): Unit =  { value = v }
      final def finish: Json = value
      final def isObj: Boolean = false
    }

    final def arrayContext(): FContext[Json] = new FContext[Json] {
      private[this] val vs = ArrayBuffer.empty[Json]
      final def add(s: String): Unit = { vs += jstring(s) }
      final def add(v: Json): Unit = { vs += v }
      final def finish: Json = Json.fromValues(vs)
      final def isObj: Boolean = false
    }

    def objectContext(): FContext[Json] = new FContext[Json] {
      private[this] final var key: String = null
      private[this] final val vs = ArrayBuffer.empty[(String, Json)]
      final def add(s: String): Unit =
        if (key == null) { key = s } else { vs += (key -> jstring(s)); key = null }
      final def add(v: Json): Unit = { vs += (key -> v); key = null }
      final def finish: Json = Json.fromFields(vs)
      final def isObj: Boolean = true
    }
  }
}
