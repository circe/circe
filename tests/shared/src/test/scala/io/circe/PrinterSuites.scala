package io.circe

import io.circe.tests.PrinterSuite

class NoSpacesPrinterSuite extends PrinterSuite(Printer.noSpaces, parser.`package`)
class Spaces2PrinterSuite extends PrinterSuite(Printer.noSpaces, parser.`package`)
class Spaces4PrinterSuite extends PrinterSuite(Printer.spaces4, parser.`package`)
