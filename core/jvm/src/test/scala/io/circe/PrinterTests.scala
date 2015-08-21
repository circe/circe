package io.circe

import argonaut._, Argonaut._
import io.circe.test.{ PrinterTests, CirceSuite }

class PrinterSuite(printer: Printer) extends CirceSuite {
  checkAll("Printing Unit", PrinterTests[Unit].printer(printer))
  checkAll("Printing Boolean", PrinterTests[Boolean].printer(printer))
  checkAll("Printing Char", PrinterTests[Char].printer(printer))
  checkAll("Printing Float", PrinterTests[Float].printer(printer))
  checkAll("Printing Double", PrinterTests[Double].printer(printer))
  checkAll("Printing Short", PrinterTests[Short].printer(printer))
  checkAll("Printing Int", PrinterTests[Int].printer(printer))
  checkAll("Printing Long", PrinterTests[Long].printer(printer))
  checkAll("Printing Map", PrinterTests[Map[String, List[Int]]].printer(printer))
}

class NoSpacesPrinterTests extends PrinterSuite(Printer.noSpaces)
class Spaces2PrinterTests extends PrinterSuite(Printer.noSpaces)
class Spaces4PrinterTests extends PrinterSuite(Printer.spaces4)
