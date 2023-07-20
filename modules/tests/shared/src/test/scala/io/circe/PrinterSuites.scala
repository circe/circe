/*
 * Copyright 2023 circe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.circe

import cats.kernel.instances.string._
import cats.syntax.eq._
import io.circe.tests.PrinterSuite

class Spaces2PrinterSuite extends PrinterSuite(Printer.spaces2, parser.`package`) with Spaces2PrinterExample
class Spaces4PrinterSuite extends PrinterSuite(Printer.spaces4, parser.`package`)
class UnicodeEscapePrinterSuite extends PrinterSuite(Printer.noSpaces.copy(escapeNonAscii = true), parser.`package`) {
  import io.circe.syntax._
  test("Printing object should unicode-escape all non-ASCII chars") {
    val actual = Json.obj("0 ℃" := "32 ℉").printWith(circePrinter)
    val expected = "{\"0 \\u2103\":\"32 \\u2109\"}"
    assertEquals(actual, expected)
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
