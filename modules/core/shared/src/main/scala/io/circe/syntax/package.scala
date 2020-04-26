package io.circe

/**
 * This package provides syntax via enrichment classes.
 */
package object syntax {
  implicit final class EncoderOps[A](private val value: A) extends AnyVal {
    @deprecated("Do not use", "0.12.3")
    def wrappedEncodeable: A = value
    final def asJson[J](implicit encoder: Encoder[A, J]): J = encoder(value)
    final def asJsonObject[J](implicit encoder: Encoder.AsObject[A, J]): JsonObject[J] =
      encoder.encodeObject(value)
  }
  implicit final class KeyOps[K](private val value: K) extends AnyVal {
    @deprecated("Do not use", "0.12.3")
    def key: K = value
    final def :=[A, J](a: A)(implicit keyEncoder: KeyEncoder[K], enc: Encoder[A, J]): (String, J) = (keyEncoder(value), a.asJson)
  }
}
