package io.circe.benchmark

import io.circe.jawn.decode
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.UTF_8
import munit.FunSuite
import cats.syntax.eq._

class PrintingBenchmarkSpec extends FunSuite {
  val benchmark: PrintingBenchmark = new PrintingBenchmark

  import benchmark._

  def byteBufferToString(buffer: ByteBuffer): String = {
    val bytes = new Array[Byte](buffer.limit)
    buffer.get(bytes)

    new String(bytes, UTF_8)
  }

  test("The string printer should correctly decode Foos") {
    assertEquals(decode[Map[String, Foo]](printFoosToString), Right(foos))
  }

  test("The string printer should correctly encode Ints") {
    assertEquals(decode[List[Int]](printIntsToString), Right(ints))
  }

  test("The string printer should correctly encode Booleans") {
    assertEquals(decode[List[Boolean]](printBooleansToString), Right(booleans))
  }

  test("The byte buffer printer should correctly decode Foos") {
    assertEquals(decode[Map[String, Foo]](byteBufferToString(printFoosToByteBuffer)), Right(foos))
  }

  test("The byte buffer printer should correctly encode Ints") {
    assertEquals(decode[List[Int]](byteBufferToString(printIntsToByteBuffer)), Right(ints))
  }

  test("The byte buffer printer should correctly encode Booleans") {
    assertEquals(decode[List[Boolean]](byteBufferToString(printBooleansToByteBuffer)), Right(booleans))
  }
}
