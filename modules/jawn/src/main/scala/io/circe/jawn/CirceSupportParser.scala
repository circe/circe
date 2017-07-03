package io.circe.jawn

import io.circe.{ Json, JsonBigDecimal, JsonBiggerDecimal, JsonObject }
import io.circe.numbers.{ BiggerDecimal, JsonNumberParser }
import java.math.{ BigDecimal, BigInteger }
import java.util.LinkedHashMap
import jawn.{ CharBasedParser, Facade, FContext, SupportParser, SyncParser }
import scala.util.Try

final object CirceSupportParser extends SupportParser[Json] {
  final override def parseUnsafe(s: String): Json = new StringParser(s).parse
  final override def parseFromString(s: String): Try[Json] = Try(new StringParser(s).parse)

  private[this] final class Slice(s: String, start: Int, limit: Int) extends CharSequence {
    final val length: Int = limit - start
    final def charAt(i: Int): Char = s.charAt(start + i)
    final def subSequence(i: Int, j: Int): CharSequence = new Slice(s, start + i, start + j)
    final override def toString: String = s.substring(start, limit)
  }

  private[this] final class StringParser(s: String) extends SyncParser[Json] with CharBasedParser[Json] {
    var line: Int = 0
    final def column(i: Int): Int = i
    final def newline(i: Int): Unit = { line += 1 }
    final def reset(i: Int): Int = i
    final def checkpoint(state: Int, i: Int, stack: List[FContext[Json]]): Unit = ()
    final def at(i: Int): Char = s.charAt(i)
    final def at(i: Int, j: Int): CharSequence = new Slice(s, i, j)
    final def atEof(i: Int): Boolean = i == s.length
    final def close(): Unit = ()
  }

  private[this] val jsonNumberParser: JsonNumberParser[Json] = new JsonNumberParser[Json] {
    final def createNegativeZeroValue: Json = Json.JNumber(JsonBiggerDecimal(BiggerDecimal.fromDoubleUnsafe(-0.0)))
    final def createUnsignedZeroValue: Json = Json.fromLong(0)
    final def createLongValue(value: Long): Json = Json.fromLong(value)
    final def createBigDecimalValue(unscaled: BigInteger, scale: Int): Json =
      Json.JNumber(JsonBigDecimal(new BigDecimal(unscaled, scale)))
    final def createBiggerDecimalValue(unscaled: BigInteger, scale: BigInteger): Json =
      Json.JNumber(JsonBiggerDecimal(BiggerDecimal(unscaled, scale)))
    final def failureValue: Json = null
  }

  implicit final val facade: Facade[Json] = new Facade[Json] {
    final def jnull(): Json = Json.Null
    final def jfalse(): Json = Json.False
    final def jtrue(): Json = Json.True
    final def jnum(s: CharSequence, decIndex: Int, expIndex: Int): Json =
      jsonNumberParser.parseUnsafeWithIndices(s, decIndex, expIndex)
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

    final def objectContext(): FContext[Json] = new FContext[Json] {
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
