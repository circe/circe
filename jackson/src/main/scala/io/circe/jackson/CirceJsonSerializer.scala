package io.circe.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.{ JsonSerializer, SerializerProvider }
import io.circe.{ Json, JsonBigDecimal, JsonBiggerDecimal, JsonDecimal, JsonDouble, JsonLong }

private[jackson] final object CirceJsonSerializer extends JsonSerializer[Json] {
  import java.math.{ BigDecimal => JBigDecimal, BigInteger }
  import com.fasterxml.jackson.databind.node.{ BigIntegerNode, DecimalNode }

  override final def serialize(
    value: Json,
    json: JsonGenerator,
    provider: SerializerProvider
  ): Unit = value match {
    case Json.JNumber(v) => v match {
      case JsonLong(x) => json.writeNumber(x)
      case JsonDouble(x) => json.writeNumber(x)
      case JsonDecimal(x) => json.writeString(x)
      case JsonBiggerDecimal(x) => json.writeString(x.toString)
      case JsonBigDecimal(x) =>
        // Workaround #3784: Same behaviour as if JsonGenerator were
        // configured with WRITE_BIGDECIMAL_AS_PLAIN, but forced as this
        // configuration is ignored when called from ObjectMapper.valueToTree
        val raw = x.bigDecimal.stripTrailingZeros.toPlainString

        if (raw contains ".") json.writeTree(new DecimalNode(new JBigDecimal(raw)))
          else json.writeTree(new BigIntegerNode(new BigInteger(raw)))
    }
    case Json.JString(v) => json.writeString(v)
    case Json.JBoolean(v) => json.writeBoolean(v)
    case Json.JArray(elements) => {
      json.writeStartArray()
      elements.foreach { t => serialize(t, json, provider) }
      json.writeEndArray()
    }
    case Json.JObject(values) => {
      json.writeStartObject()
      values.toList.foreach { t =>
        json.writeFieldName(t._1)
        serialize(t._2, json, provider)
      }
      json.writeEndObject()
    }
    case Json.JNull => json.writeNull()
  }
}
