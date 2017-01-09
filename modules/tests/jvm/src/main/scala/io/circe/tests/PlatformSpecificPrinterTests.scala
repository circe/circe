package io.circe.tests

import io.circe.Json
import io.circe.testing.PrinterTests
import java.nio.charset.StandardCharsets.UTF_8

trait PlatformSpecificPrinterTests { self: PrinterSuite =>
  // Temporarily JVM-only because of problems round-tripping in Scala.js.
  checkLaws("Printing Long", PrinterTests[Long].printer(printer, parser))

  "prettyByteBuffer" should "match pretty" in forAll { (json: Json) =>
    val buffer = printer.prettyByteBuffer(json)

    val bytes = new Array[Byte](buffer.limit)
    buffer.get(bytes)

    val asString = new String(bytes, UTF_8)
    val expected = printer.pretty(json)

    assert(asString === expected)
  }
}
