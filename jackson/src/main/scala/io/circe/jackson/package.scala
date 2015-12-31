package io.circe

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.ObjectWriter

/**
 * Support for Jackson-powered parsing and printing for circe.
 *
 * The implementation is ported with minimal changes from Play JSON.
 */
package object jackson extends WithJacksonMapper with JacksonParser {
  def jacksonPrint(json: Json): String = {
    val sw = new java.io.StringWriter
    val gen = stringJsonGenerator(sw).setPrettyPrinter(
      new DefaultPrettyPrinter()
    )
    val writer: ObjectWriter = mapper.writerWithDefaultPrettyPrinter[ObjectWriter]()
    writer.writeValue(gen, json)
    sw.flush()
    sw.getBuffer.toString
  }
}
