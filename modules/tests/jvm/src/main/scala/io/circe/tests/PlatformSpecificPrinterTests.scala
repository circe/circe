package io.circe.tests

import cats.kernel.instances.long._
import io.circe.testing.PrinterTests

trait PlatformSpecificPrinterTests { self: PrinterSuite =>
  // Temporarily JVM-only because of problems round-tripping in Scala.js.
  checkAll("Printing Long", PrinterTests[Long].printer(printer, parser))
}
