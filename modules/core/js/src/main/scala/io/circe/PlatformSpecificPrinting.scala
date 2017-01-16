package io.circe

import java.io.{ ByteArrayOutputStream, Writer }
import java.nio.ByteBuffer

abstract class PlatformSpecificPrinting {
  protected[this] class EnhancedByteArrayOutputStream extends ByteArrayOutputStream {
    def toByteBuffer: ByteBuffer = ByteBuffer.wrap(this.toByteArray)
  }

  def bufferWriter(writer: Writer): Writer = writer
}
