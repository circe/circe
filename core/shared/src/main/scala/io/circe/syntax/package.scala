package io.circe

/**
 * This package provides syntax via enrichment classes.
 */
package object syntax {
  implicit final class EncoderOps[A](val a: A) extends AnyVal {
    final def asJson(implicit e: Encoder[A]): Json = e(a)
  }
}
