package io.circe
import java.nio.ByteBuffer
import java.nio.charset.Charset

trait Printer {

  /**
    * Returns a string representation of a pretty-printed JSON value.
    */
  def pretty(json: Json): String

  def prettyByteBuffer(json: Json, cs: Charset): ByteBuffer

  def prettyByteBuffer(json: Json): ByteBuffer
}

trait PrinterBuilder {
  def noSpaces: Printer

  def spaces2: Printer
}
