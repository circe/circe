package io.circe.printer

import io.circe.PrinterBuilder

object Implicits {
  implicit val printerBuilder: PrinterBuilder = Printer
}
