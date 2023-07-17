package io.circe.tests

import cats.kernel.instances.all._
import io.circe.{ Json, Parser, Printer }
import io.circe.testing.PrinterTests
import java.nio.charset.StandardCharsets.UTF_8
import org.scalacheck.Prop._

class PrinterSuite(val circePrinter: Printer, val parser: Parser)
    extends CirceMunitSuite
    with PlatformSpecificPrinterTests {

  checkAll("Printing Unit", PrinterTests[Unit].printer(circePrinter, parser))
  checkAll("Printing Boolean", PrinterTests[Boolean].printer(circePrinter, parser))
  checkAll("Printing Char", PrinterTests[Char].printer(circePrinter, parser))
  checkAll("Printing Float", PrinterTests[Float].printer(circePrinter, parser))
  checkAll("Printing Double", PrinterTests[Double].printer(circePrinter, parser))
  checkAll("Printing Short", PrinterTests[Short].printer(circePrinter, parser))
  checkAll("Printing Int", PrinterTests[Int].printer(circePrinter, parser))
  checkAll("Printing Map", PrinterTests[Map[String, List[Int]]].printer(circePrinter, parser))

  property("printToByteBuffer should match print")(printToBufferProp)
  private lazy val printToBufferProp = forAll { (json: Json, predictSize: Boolean) =>
    val buffer = circePrinter.copy(predictSize = predictSize).printToByteBuffer(json)

    val bytes = new Array[Byte](buffer.limit)
    buffer.get(bytes)

    val asString = new String(bytes, UTF_8)
    val expected = circePrinter.print(json)

    assert(asString == expected)
  }
}
