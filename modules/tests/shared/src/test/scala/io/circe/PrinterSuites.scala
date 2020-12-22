package io.circe

import cats.kernel.instances.string._
import cats.syntax.eq._
import io.circe.tests.PrinterSuite
import org.scalacheck.Prop.forAll

class Spaces2PrinterSuite extends PrinterSuite(Printer.spaces2, parser.`package`) with Spaces2PrinterExample
class Spaces4PrinterSuite extends PrinterSuite(Printer.spaces4, parser.`package`)
class NoSpacesPrinterSuite extends PrinterSuite(Printer.noSpaces, parser.`package`) {
  property("pretty should match print") {
    forAll { (json: Json) =>
      assert(printer.print(json) === printer.pretty(json))
    }
  }
}
class UnicodeEscapePrinterSuite extends PrinterSuite(Printer.noSpaces.copy(escapeNonAscii = true), parser.`package`) {
  import io.circe.syntax._
  test("Printing object should unicode-escape all non-ASCII chars") {
    val actual = Json.obj("0 ℃" := "32 ℉").printWith(printer)
    val expected = "{\"0 \\u2103\":\"32 \\u2109\"}"
    assert(actual === expected)
  }
}

class Spaces2PrinterWithWriterReuseSuite
    extends PrinterSuite(
      Printer.spaces2.copy(reuseWriters = true),
      parser.`package`
    )

class Spaces4PrinterWithWriterReuseSuite
    extends PrinterSuite(
      Printer.spaces4.copy(reuseWriters = true),
      parser.`package`
    )

class NoSpacesPrinterWithWriterReuseSuite
    extends PrinterSuite(
      Printer.noSpaces.copy(reuseWriters = true),
      parser.`package`
    )

class UnicodeEscapePrinterWithWriterReuseSuite
    extends PrinterSuite(
      Printer.noSpaces.copy(reuseWriters = true, escapeNonAscii = true),
      parser.`package`
    )

class Spaces2SortKeysPrinterSuite extends PrinterSuite(Printer.spaces2SortKeys, parser.`package`) with SortedKeysSuite
class Spaces4SortKeysPrinterSuite extends PrinterSuite(Printer.spaces4SortKeys, parser.`package`) with SortedKeysSuite
class NoSpacesSortKeysPrinterSuite extends PrinterSuite(Printer.noSpacesSortKeys, parser.`package`) with SortedKeysSuite
class CustomIndentWithSortKeysPrinterSuite
    extends PrinterSuite(Printer.indented("   ").withSortedKeys, parser.`package`)
    with SortedKeysSuite
