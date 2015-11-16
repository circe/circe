package io.circe.jackson

import io.circe.Json

/**
 * This package provides syntax for Jackson printing via enrichment classes.
 */
package object syntax {
  implicit class JacksonPrintingOps[A](val json: Json) extends AnyVal {
    def jacksonPrint: String = io.circe.jackson.jacksonPrint(json)
  }
}
