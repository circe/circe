package io.circe.jackson

import cats.data.Xor
import com.fasterxml.jackson.core.{ JsonParser, JsonTokenId }
import com.fasterxml.jackson.databind.{ DeserializationContext, JsonDeserializer }
import com.fasterxml.jackson.databind.`type`.TypeFactory
import io.circe.{ Json, JsonBigDecimal, JsonObject }
import java.util.ArrayList
import scala.annotation.{ switch, tailrec }

private[jackson] sealed trait DeserializerContext {
  def addValue(value: Json): DeserializerContext
}

private[jackson] case class ReadingList(content: ArrayList[Json]) extends DeserializerContext {
  def addValue(value: Json): DeserializerContext = ReadingList { content.add(value); content }
}

private[jackson] case class KeyRead(content: ArrayList[(String, Json)], fieldName: String)
  extends DeserializerContext {
  def addValue(value: Json): DeserializerContext = ReadingMap {
    content.add(fieldName -> value)
    content
  }
}

private[jackson] case class ReadingMap(content: ArrayList[(String, Json)])
  extends DeserializerContext {
  def setField(fieldName: String): DeserializerContext = KeyRead(content, fieldName)
  def addValue(value: Json): DeserializerContext =
    throw new Exception("Cannot add a value on an object without a key, malformed JSON object!")
}

private[jackson] class CirceJsonDeserializer(factory: TypeFactory, klass: Class[_])
  extends JsonDeserializer[Object] {
  override def isCachable: Boolean = true

  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): Json = {
    val value = deserialize(jp, ctxt, List())
    if (!klass.isAssignableFrom(value.getClass)) throw ctxt.mappingException(klass)

    value
  }

  @tailrec
  final def deserialize(
    jp: JsonParser,
    ctxt: DeserializationContext,
    parserContext: List[DeserializerContext]
  ): Json = {
    if (jp.getCurrentToken == null) jp.nextToken()

    val (maybeValue, nextContext) = (jp.getCurrentToken.id(): @switch) match {
      case JsonTokenId.ID_NUMBER_INT | JsonTokenId.ID_NUMBER_FLOAT =>
        (Some(Json.JNumber(JsonBigDecimal(new BigDecimal(jp.getDecimalValue)))), parserContext)

      case JsonTokenId.ID_STRING => (Some(Json.JString(jp.getText)), parserContext)
      case JsonTokenId.ID_TRUE => (Some(Json.JBoolean(true)), parserContext)
      case JsonTokenId.ID_FALSE => (Some(Json.JBoolean(false)), parserContext)
      case JsonTokenId.ID_NULL => (Some(Json.JNull), parserContext)
      case JsonTokenId.ID_START_ARRAY => (None, ReadingList(new ArrayList) +: parserContext)

      case JsonTokenId.ID_END_ARRAY => parserContext match {
        case ReadingList(content) :: stack =>
          (Some(Json.JArray(content.toArray(new Array[Json](content.size)))), stack)
        case _ => throw new RuntimeException("We weren't reading a list, something went wrong")
      }

      case JsonTokenId.ID_START_OBJECT => (None, ReadingMap(new ArrayList) +: parserContext)

      case JsonTokenId.ID_FIELD_NAME => parserContext match {
        case (c: ReadingMap) :: stack => (None, c.setField(jp.getCurrentName) +: stack)
        case _ => throw new RuntimeException("We weren't reading an object, something went wrong")
      }

      case JsonTokenId.ID_END_OBJECT => parserContext match {
        case ReadingMap(content) :: stack => (
          Some(
            Json.JObject(
              JsonObject.fromIndexedSeq(content.toArray(new Array[(String, Json)](content.size)))
            )
          ),
          stack
        )
        case _ => throw new RuntimeException("We weren't reading an object, something went wrong")
      }

      case JsonTokenId.ID_NOT_AVAILABLE =>
        throw new RuntimeException("We weren't reading an object, something went wrong")

      case JsonTokenId.ID_EMBEDDED_OBJECT =>
        throw new RuntimeException("We weren't reading an object, something went wrong")
    }

    jp.nextToken()

    maybeValue match {
      case Some(v) if nextContext.isEmpty => v
      case maybeValue =>
        val toPass = maybeValue.map { v =>
          val previous :: stack = nextContext
          (previous.addValue(v)) +: stack
        }.getOrElse(nextContext)

        deserialize(jp, ctxt, toPass)
    }
  }

  override def getNullValue = Json.JNull
}
