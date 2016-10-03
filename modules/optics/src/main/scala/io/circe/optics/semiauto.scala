package io.circe.optics

import io.circe.{ Decoder, Encoder }
import monocle.Iso

/**
 * Semi-automatic codec derivation with monocle.Iso.
 *
 * This object provides helpers for creating [[io.circe.Decoder]][B]/[[io.circe.Encoder]][B]
 * instances when there already is Decoder[A]/Encoder[A] and Iso[A, B] or Iso[B, A].
 */
final object semiauto {
  final def deriveDecoderWithIso[A, B](implicit decoder: Decoder[A], iso: Iso[A, B]): Decoder[B] = decoder.map(iso.get)

  final def deriveEncoderWithIso[A, B](implicit encoder: Encoder[A], iso: Iso[A, B]): Encoder[B] = encoder.contramap(iso.reverseGet)

  final def deriveDecoderWithIso[B, A](implicit decoder: Decoder[A], iso: Iso[B, A], e: DummyImplicit): Decoder[B] = decoder.map(iso.reverseGet)

  final def deriveEncoderWithIso[B, A](implicit encoder: Encoder[A], iso: Iso[B, A], e: DummyImplicit): Encoder[B] = encoder.contramap(iso.get)
}
