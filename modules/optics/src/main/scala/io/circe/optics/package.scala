package io.circe

import monocle.Iso

package optics {
  final object all extends JsonNumberOptics with JsonObjectOptics with JsonOptics
}

package object optics {
  /** Derives a [[io.circe.Decoder]][B] from a Decoder[A] and [[monocle.Iso]][A, B].
   *
   *  @tparam A the type parameter of the existent Decoder.
   *  @tparam B the type parameter of the derived Decoder.
   *  @param decoder the existent Decoder.
   *  @param iso the existent Iso.
   */
  final def deriveDecoderWithIso[A, B](implicit decoder: Decoder[A], iso: Iso[A, B]): Decoder[B] =
    decoder.map(iso.get)

  /** Derives an [[io.circe.Encoder]][B] from an Encoder[A] and [[monocle.Iso]][A, B].
   *
   *  @tparam A the type parameter of the existent Encoder.
   *  @tparam B the type parameter of the derived Encoder.
   *  @param encoder the existent Encoder.
   *  @param iso the existent Iso.
   */
  final def deriveEncoderWithIso[A, B](implicit encoder: Encoder[A], iso: Iso[A, B]): Encoder[B] =
    encoder.contramap(iso.reverseGet)

  /** Derives a [[io.circe.Decoder]][B] from a Decoder[A] and [[monocle.Iso]][B, A].
   *
   *  @tparam A the type parameter of the existent Decoder.
   *  @tparam B the type parameter of the derived Decoder.
   *  @param decoder the existent Decoder.
   *  @param iso the existent Iso.
   */
  final def deriveDecoderWithIsoReverse[B, A](implicit decoder: Decoder[A], iso: Iso[B, A]): Decoder[B] =
    decoder.map(iso.reverseGet)

  /** Derives an [[io.circe.Encoder]][B] from an Encoder[A] and [[monocle.Iso]][B, A].
   *
   *  @tparam A the type parameter of the existent Encoder.
   *  @tparam B the type parameter of the derived Encoder.
   *  @param encoder the existent Encoder.
   *  @param iso the existent Iso.
   */
  final def deriveEncoderWithIsoReverse[B, A](implicit encoder: Encoder[A], iso: Iso[B, A]): Encoder[B] =
    encoder.contramap(iso.get)
}
