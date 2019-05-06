package io.circe.benchmark

import io.circe.jawn.decode
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.UTF_8
import org.scalacheck.Properties
import org.typelevel.claimant.Claim

class PrintingBenchmarkSuite extends Properties("PrintingBenchmark") {
  val benchmark: PrintingBenchmark = new PrintingBenchmark

  import benchmark._

  def byteBufferToString(buffer: ByteBuffer): String = {
    val bytes = new Array[Byte](buffer.limit)
    buffer.get(bytes)

    new String(bytes, UTF_8)
  }

  property("The string printer should correctly decode Foos") = Claim(
    decode[Map[String, Foo]](printFoosToString) == Right(foos)
  )

  property("The string printer should correctly encode Ints") = Claim(
    decode[List[Int]](printIntsToString) == Right(ints)
  )

  property("The string printer should correctly encode Booleans") = Claim(
    decode[List[Boolean]](printBooleansToString) == Right(booleans)
  )

  property("The byte buffer printer should correctly decode Foos") = Claim(
    decode[Map[String, Foo]](byteBufferToString(printFoosToByteBuffer)) == Right(foos)
  )

  property("The byte buffer printer should correctly encode Ints") = Claim(
    decode[List[Int]](byteBufferToString(printIntsToByteBuffer)) == Right(ints)
  )

  property("The byte buffer printer should correctly encode Booleans") = Claim(
    decode[List[Boolean]](byteBufferToString(printBooleansToByteBuffer)) == Right(booleans)
  )
}
