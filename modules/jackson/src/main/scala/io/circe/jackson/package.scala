package io.circe

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter

/**
 * Support for Jackson-powered parsing and printing for circe.
 *
 * Note that not all guarantees that hold for Jawn-based parsing and the default
 * printer will hold for the Jackson-based versions. Jackson's handling of
 * numbers in particular differs significantly: it doesn't distinguish positive
 * and negative zeros, it may truncate large JSON numbers or simply fail to
 * parse them, it may print large numbers as strings, etc.
 *
 * The implementation is ported with minimal changes from Play JSON.
 */
package object jackson extends WithJacksonMapper with JacksonParser {
  final def jacksonPrint(json: Json): String = {
    val sw = new java.io.StringWriter
    val gen = stringJsonGenerator(sw).setPrettyPrinter(
      new DefaultPrettyPrinter()
    )
    val writer = mapper.writerWithDefaultPrettyPrinter()
    writer.writeValue(gen, json)
    sw.flush()
    sw.getBuffer.toString
  }
}
