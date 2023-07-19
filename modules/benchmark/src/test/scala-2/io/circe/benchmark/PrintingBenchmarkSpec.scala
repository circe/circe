/*
 * Copyright 2023 circe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
