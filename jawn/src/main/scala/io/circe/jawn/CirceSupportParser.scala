package io.circe.jawn

import io.circe.{ Json, JsonDecimal }
import jawn.{ FContext, Facade, SupportParser }
import scala.collection.mutable.ArrayBuffer

object CirceSupportParser extends SupportParser[Json] {
  implicit val facade: Facade[Json] = new Facade[Json] {
    def jnull(): Json = Json.Empty
    def jfalse(): Json = Json.False
    def jtrue(): Json = Json.True
    def jnum(s: String): Json = Json.JNumber(JsonDecimal(s))
    def jint(s: String): Json = Json.JNumber(JsonDecimal(s))
    def jstring(s: String): Json = Json.JString(s)

    def singleContext(): FContext[Json] = new FContext[Json] {
      private[this] var value: Json = null
      def add(s: String): Unit = { value = jstring(s) }
      def add(v: Json): Unit =  { value = v }
      def finish: Json = value
      def isObj: Boolean = false
    }

    def arrayContext(): FContext[Json] = new FContext[Json] {
      private[this] val vs = ArrayBuffer.empty[Json]
      def add(s: String): Unit = { vs += jstring(s) }
      def add(v: Json): Unit = { vs += v }
      def finish: Json = Json.fromValues(vs)
      def isObj: Boolean = false
    }

    def objectContext(): FContext[Json] = new FContext[Json] {
      private[this] var key: String = null
      private[this] val vs = ArrayBuffer.empty[(String, Json)]
      def add(s: String): Unit =
        if (key == null) { key = s } else { vs += (key -> jstring(s)); key = null }
      def add(v: Json): Unit = { vs += (key -> v); key = null }
      def finish: Json = Json.fromFields(vs)
      def isObj: Boolean = true
    }
  }
}
