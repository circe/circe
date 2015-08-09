package io.circe

/**
 * This package provides syntax via enrichment classes.
 */
package object syntax {
  implicit class EncoderOps[A](val a: A) extends AnyVal {
    def asJson(implicit e: Encoder[A]): Json = e(a)
  }
}
