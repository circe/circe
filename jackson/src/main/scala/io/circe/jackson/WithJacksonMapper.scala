package io.circe.jackson

import com.fasterxml.jackson.core.{ JsonFactory, JsonParser }
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.{ File, StringWriter }

class WithJacksonMapper {
  protected val mapper: ObjectMapper = (new ObjectMapper).registerModule(CirceJsonModule)
  private[this] val jsonFactory: JsonFactory = new JsonFactory(mapper)

  protected def jsonStringParser(input: String): JsonParser = jsonFactory.createParser(input)
  protected def jsonFileParser(file: File): JsonParser = jsonFactory.createParser(file)
  protected def jsonBytesParser(bytes: Array[Byte]): JsonParser = jsonFactory.createParser(bytes)
  protected def stringJsonGenerator(out: StringWriter) = jsonFactory.createGenerator(out)
}
