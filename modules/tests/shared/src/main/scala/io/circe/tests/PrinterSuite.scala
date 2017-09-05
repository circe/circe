package io.circe.tests

import io.circe.{ Json, Parser, Printer }
import io.circe.testing.PrinterTests
import java.nio.charset.StandardCharsets.UTF_8

class PrinterSuite(val printer: Printer, val parser: Parser) extends CirceSuite with PlatformSpecificPrinterTests {
  checkLaws("Printing Unit", PrinterTests[Unit].printer(printer, parser))
  checkLaws("Printing Boolean", PrinterTests[Boolean].printer(printer, parser))
  checkLaws("Printing Char", PrinterTests[Char].printer(printer, parser))
  checkLaws("Printing Float", PrinterTests[Float].printer(printer, parser))
  checkLaws("Printing Double", PrinterTests[Double].printer(printer, parser))
  checkLaws("Printing Short", PrinterTests[Short].printer(printer, parser))
  checkLaws("Printing Int", PrinterTests[Int].printer(printer, parser))
  checkLaws("Printing Map", PrinterTests[Map[String, List[Int]]].printer(printer, parser))

  "prettyByteBuffer" should "match pretty" in forAll { (json: Json, predictSize: Boolean) =>
    val buffer = printer.copy(predictSize = predictSize).prettyByteBuffer(json)

    val bytes = new Array[Byte](buffer.limit)
    buffer.get(bytes)

    val asString = new String(bytes, UTF_8)
    val expected = printer.pretty(json)

    assert(asString === expected)
  }
}
