package io.circe

import java.io.{ BufferedWriter, ByteArrayOutputStream, Writer }
import java.nio.ByteBuffer

abstract class PlatformSpecificPrinting {
  protected[this] class EnhancedByteArrayOutputStream extends ByteArrayOutputStream {
    def toByteBuffer: ByteBuffer = ByteBuffer.wrap(this.buf, 0, this.size)
  }

  def bufferWriter(writer: Writer): Writer = new BufferedWriter(writer)
}
