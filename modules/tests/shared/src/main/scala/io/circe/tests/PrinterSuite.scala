package io.circe.tests

import io.circe.{ Parser, Printer }
import io.circe.testing.PrinterTests

class PrinterSuite(val printer: Printer, val parser: Parser) extends CirceSuite with PlatformSpecificPrinterTests {
  checkLaws("Printing Unit", PrinterTests[Unit].printer(printer, parser))
  checkLaws("Printing Boolean", PrinterTests[Boolean].printer(printer, parser))
  checkLaws("Printing Char", PrinterTests[Char].printer(printer, parser))
  checkLaws("Printing Float", PrinterTests[Float].printer(printer, parser))
  checkLaws("Printing Double", PrinterTests[Double].printer(printer, parser))
  checkLaws("Printing Short", PrinterTests[Short].printer(printer, parser))
  checkLaws("Printing Int", PrinterTests[Int].printer(printer, parser))
  checkLaws("Printing Map", PrinterTests[Map[String, List[Int]]].printer(printer, parser))
}
