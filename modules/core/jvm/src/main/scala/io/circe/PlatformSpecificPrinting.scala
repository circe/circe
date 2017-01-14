package io.circe

import java.io.{ BufferedWriter, Writer }

abstract class PlatformSpecificPrinting {
  def bufferWriter(writer: Writer): Writer = new BufferedWriter(writer)
}
