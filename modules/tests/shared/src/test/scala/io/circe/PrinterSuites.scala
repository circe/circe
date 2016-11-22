package io.circe

import io.circe.ast.Printer
import io.circe.tests.PrinterSuite

class Spaces2PrinterSuite extends PrinterSuite(Printer.spaces2, parser.`package`) with Spaces2PrinterExample
class Spaces4PrinterSuite extends PrinterSuite(Printer.spaces4, parser.`package`)
class NoSpacesPrinterSuite extends PrinterSuite(Printer.noSpaces, parser.`package`)
