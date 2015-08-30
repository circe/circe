package io.circe

import io.circe.test.PrinterSuite

/**
 * Tests for [[io.circe.Printer]].
 *
 * This isn't the ideal place for these tests, but we don't have a parser available in the core
 * package, and it's not possible to include e.g. an Argonaut dependency there if we want to support
 * Scala.js testing
 */
class NoSpacesPrinterTests extends PrinterSuite(Printer.noSpaces, parse.`package`)
class Spaces2PrinterTests extends PrinterSuite(Printer.noSpaces, parse.`package`)
class Spaces4PrinterTests extends PrinterSuite(Printer.spaces4, parse.`package`)
