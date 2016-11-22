package io.circe.tests

import io.circe.Parser
import io.circe.ast.Printer
import io.circe.testing.PrinterTests

class PrinterSuite(printer: Printer, parser: Parser) extends CirceSuite {
  checkLaws("Printing Unit", PrinterTests[Unit].printer(printer, parser))
  checkLaws("Printing Boolean", PrinterTests[Boolean].printer(printer, parser))
  checkLaws("Printing Char", PrinterTests[Char].printer(printer, parser))
  checkLaws("Printing Float", PrinterTests[Float].printer(printer, parser))
  checkLaws("Printing Double", PrinterTests[Double].printer(printer, parser))
  checkLaws("Printing Short", PrinterTests[Short].printer(printer, parser))
  checkLaws("Printing Int", PrinterTests[Int].printer(printer, parser))
  // Temporarily disabling because of problems round-tripping in Scala.js.
  //checkLaws("Printing Long", PrinterTests[Long].printer(printer, parser))
  checkLaws("Printing Map", PrinterTests[Map[String, List[Int]]].printer(printer, parser))
}
