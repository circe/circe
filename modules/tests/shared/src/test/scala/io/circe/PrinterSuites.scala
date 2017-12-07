package io.circe

import io.circe.tests.PrinterSuite

class Spaces2PrinterSuite extends PrinterSuite(Printer.spaces2, parser.`package`) with Spaces2PrinterExample
class Spaces4PrinterSuite extends PrinterSuite(Printer.spaces4, parser.`package`)
class NoSpacesPrinterSuite extends PrinterSuite(Printer.noSpaces, parser.`package`)
class UnicodeEscapePrinterSuite extends PrinterSuite(Printer.noSpaces.copy(escapeNonAscii = true), parser.`package`) {
  import io.circe.syntax._
  "Printing object" should "unicode-escape all non-ASCII chars" in {
    val actual = Json.obj("0 ℃" := "32 ℉").pretty(printer)
    val expected = "{\"0 \\u2103\":\"32 \\u2109\"}"
    assert(actual === expected)
  }
}

class Spaces2PrinterWithWriterReuseSuite extends PrinterSuite(
  Printer.spaces2.copy(reuseWriters = true),
  parser.`package`
)

class Spaces4PrinterWithWriterReuseSuite extends PrinterSuite(
  Printer.spaces4.copy(reuseWriters = true),
  parser.`package`
)

class NoSpacesPrinterWithWriterReuseSuite extends PrinterSuite(
  Printer.noSpaces.copy(reuseWriters = true),
  parser.`package`
)

class UnicodeEscapePrinterWithWriterReuseSuite extends PrinterSuite(
  Printer.noSpaces.copy(reuseWriters = true, escapeNonAscii = true),
  parser.`package`
)
