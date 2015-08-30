package io.circe.tests

import io.circe.{ Parser, Printer }

class PrinterSuite(printer: Printer, parser: Parser) extends CirceSuite {
  checkAll("Printing Unit", PrinterTests[Unit].printer(printer, parser))
  checkAll("Printing Boolean", PrinterTests[Boolean].printer(printer, parser))
  checkAll("Printing Char", PrinterTests[Char].printer(printer, parser))
  checkAll("Printing Float", PrinterTests[Float].printer(printer, parser))
  checkAll("Printing Double", PrinterTests[Double].printer(printer, parser))
  checkAll("Printing Short", PrinterTests[Short].printer(printer, parser))
  checkAll("Printing Int", PrinterTests[Int].printer(printer, parser))
  // Temporarily disabling because of problems round-tripping in Scala.js. 
  //checkAll("Printing Long", PrinterTests[Long].printer(printer, parser))
  checkAll("Printing Map", PrinterTests[Map[String, List[Int]]].printer(printer, parser))
}
