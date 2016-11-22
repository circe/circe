package io.circe.jackson

import io.circe.ast.Json

/**
 * This package provides syntax for Jackson printing via enrichment classes.
 */
package object syntax {
  implicit final class JacksonPrintingOps[A](val json: Json) extends AnyVal {
    final def jacksonPrint: String = io.circe.jackson.jacksonPrint(json)
  }
}
