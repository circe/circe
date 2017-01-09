package io.circe

import java.io.{ BufferedWriter, ByteArrayOutputStream, OutputStreamWriter }
import java.nio.ByteBuffer

abstract class PlatformSpecificPrinting { Printer =>
  protected[this] def printJsonAtDepth(writer: Appendable)(json: Json, depth: Int): Unit

  private[this] class EnhancedByteArrayOutputStream extends ByteArrayOutputStream {
    def toByteBuffer: ByteBuffer = ByteBuffer.wrap(this.buf, 0, this.size)
  }

  final def prettyByteBuffer(json: Json): ByteBuffer = {
    val bytes = new EnhancedByteArrayOutputStream
    val writer = new BufferedWriter(new OutputStreamWriter(bytes, "UTF-8"))

    printJsonAtDepth(writer)(json, 0)

    writer.close()
    bytes.toByteBuffer
  }
}
