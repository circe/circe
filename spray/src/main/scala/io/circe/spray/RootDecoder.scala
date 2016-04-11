package io.circe.spray

import io.circe.{ Decoder, Encoder, RootEncoder }

/**
 * A type class that wraps a decoder that will only accept root JSON values
 * (i.e., either JSON objects or arrays).
 *
 * Because the core circe module does not provide subtypes of [[Decoder]] that
 * provide this guarantee, we approximate it by creating instances for types
 * that have both [[Decoder]] and [[RootEncoder]] instances, or that only have
 * a [[Decoder]] instance (and no [[Encoder]]).
 */
case class RootDecoder[A](underlying: Decoder[A])

final object RootDecoder extends LowPriorityRootDecoders {
  implicit def rootDecoderWithRootEncoder[A](implicit decoder: Decoder[A], encoder: RootEncoder[A]): RootDecoder[A] =
    RootDecoder(decoder)
}

private[spray] sealed class LowPriorityRootDecoders {
  implicit def rootDecoderBlocker1[A](implicit decoder: Decoder[A], encoder: Encoder[A]): RootDecoder[A] =
    RootDecoder(decoder)
  implicit def rootDecoderBlocker2[A](implicit decoder: Decoder[A], encoder: Encoder[A]): RootDecoder[A] =
    RootDecoder(decoder)
  implicit def rootDecoder[A](implicit decoder: Decoder[A]): RootDecoder[A] = RootDecoder(decoder)
}
