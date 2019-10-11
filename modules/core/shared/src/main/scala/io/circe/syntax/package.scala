package io.circe

/**
 * This package provides syntax via enrichment classes.
 */
package object syntax {
  implicit final class EncoderOps[A](private val wrappedEncodeable: A) extends AnyVal {
    final def asJson(implicit encoder: Encoder[A]): Json = encoder(wrappedEncodeable)
    final def asJsonObject(implicit encoder: Encoder.AsObject[A]): JsonObject =
      encoder.encodeObject(wrappedEncodeable)
  }
  implicit final class KeyOps[K](private val key: K) extends AnyVal {
    final def :=[A: Encoder](a: A)(implicit keyEncoder: KeyEncoder[K]): (String, Json) = (keyEncoder(key), a.asJson)
  }
}
