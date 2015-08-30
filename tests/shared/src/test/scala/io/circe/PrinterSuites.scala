package io.circe

import io.circe.tests.PrinterSuite

class NoSpacesPrinterSuite extends PrinterSuite(Printer.noSpaces, parse.`package`)
class Spaces2PrinterSuite extends PrinterSuite(Printer.noSpaces, parse.`package`)
class Spaces4PrinterSuite extends PrinterSuite(Printer.spaces4, parse.`package`)
