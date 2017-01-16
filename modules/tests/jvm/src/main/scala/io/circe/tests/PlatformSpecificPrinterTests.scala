package io.circe.tests

import io.circe.testing.PrinterTests

trait PlatformSpecificPrinterTests { self: PrinterSuite =>
  // Temporarily JVM-only because of problems round-tripping in Scala.js.
  checkLaws("Printing Long", PrinterTests[Long].printer(printer, parser))
}
