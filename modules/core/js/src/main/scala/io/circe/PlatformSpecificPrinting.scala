package io.circe

import java.io.Writer

abstract class PlatformSpecificPrinting {
  def bufferWriter(writer: Writer): Writer = writer
}
