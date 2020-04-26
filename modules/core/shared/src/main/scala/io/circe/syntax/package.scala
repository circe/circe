package io.circe

/**
 * This package provides syntax via enrichment classes.
 */
package object syntax {
  implicit final class EncoderOps[A](private val value: A) extends AnyVal {
    @deprecated("Do not use", "0.12.3")
    def wrappedEncodeable: A = value
    final def asJson(implicit encoder: Encoder[A]): Json = encoder(value)
    final def asJsonObject(implicit encoder: Encoder.AsObject[A]): JsonObject[Json] =
      encoder.encodeObject(value)
  }
  implicit final class KeyOps[K](private val value: K) extends AnyVal {
    @deprecated("Do not use", "0.12.3")
    def key: K = value
    final def :=[A: Encoder](a: A)(implicit keyEncoder: KeyEncoder[K]): (String, Json) = (keyEncoder(value), a.asJson)
  }
}
