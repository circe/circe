package io.circe

import io.circe.tests.PrinterSuite

class Spaces2PrinterSuite extends PrinterSuite(Printer.spaces2, parser.`package`) with Spaces2PrinterExample
class Spaces4PrinterSuite extends PrinterSuite(Printer.spaces4, parser.`package`)
class NoSpacesPrinterSuite extends PrinterSuite(Printer.noSpaces, parser.`package`)

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
