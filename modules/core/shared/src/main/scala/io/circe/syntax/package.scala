package io.circe

/**
 * This package provides syntax via enrichment classes.
 */
package object syntax {
  implicit final class EncoderOps[A](val wrappedEncodeable: A) extends AnyVal {
    final def asJson(implicit encoder: Encoder[A]): Json = encoder(wrappedEncodeable)
    final def asJsonObject(implicit encoder: ObjectEncoder[A]): JsonObject =
      encoder.encodeObject(wrappedEncodeable)
  }
  implicit final class StringOps(val value: String) extends AnyVal {
    final def :=[A: Encoder](a: A): (String, Json) = (value, a.asJson)
  }
}
