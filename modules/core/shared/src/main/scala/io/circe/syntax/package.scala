package io.circe

/**
 * This package provides syntax via enrichment classes.
 */
package object syntax {
  implicit final class EncoderOps[A](private val value: A) extends AnyVal {
    final def asJson(implicit encoder: Encoder[A]): Json = encoder(value)
    final def asJsonObject(implicit encoder: Encoder.AsObject[A]): JsonObject =
      encoder.encodeObject(value)
  }
  implicit final class KeyOps[K](private val value: K) extends AnyVal {
    final def :=[A: Encoder](a: A)(implicit keyEncoder: KeyEncoder[K]): (String, Json) = (keyEncoder(value), a.asJson)
  }
}
