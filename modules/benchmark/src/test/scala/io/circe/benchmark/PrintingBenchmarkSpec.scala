package io.circe.benchmark

import io.circe.jawn.decode
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.UTF_8
import org.scalatest.FlatSpec

class PrintingBenchmarkSpec extends FlatSpec {
  val benchmark: PrintingBenchmark = new PrintingBenchmark

  import benchmark._

  def byteBufferToString(buffer: ByteBuffer): String = {
    val bytes = new Array[Byte](buffer.limit)
    buffer.get(bytes)

    new String(bytes, UTF_8)
  }

  "The string printer" should "correctly decode Foos" in {
    assert(decode[Map[String, Foo]](printFoosToString) === Right(foos))
  }

  it should "correctly encode Ints" in {
    assert(decode[List[Int]](printIntsToString) === Right(ints))
  }

  it should "correctly encode Booleans" in {
    assert(decode[List[Boolean]](printBooleansToString) === Right(booleans))
  }

  "The byte buffer printer" should "correctly decode Foos" in {
    assert(decode[Map[String, Foo]](byteBufferToString(printFoosToByteBuffer)) === Right(foos))
  }

  it should "correctly encode Ints" in {
    assert(decode[List[Int]](byteBufferToString(printIntsToByteBuffer)) === Right(ints))
  }

  it should "correctly encode Booleans" in {
    assert(decode[List[Boolean]](byteBufferToString(printBooleansToByteBuffer)) === Right(booleans))
  }
}
