package io.circe

import cats.Foldable

class EncoderCompanionOps(val underlying: Encoder.type) extends AnyVal {

  /**
   * Construct an instance for a given type with a [[cats.Foldable]] instance.
   *
   * @group Utilities
   */
  def fromFoldable[F[_], A](implicit e: Encoder[A], F: Foldable[F]): Encoder[F[A]] =
    Encoder.instance(fa =>
      Json.fromValues(F.foldLeft(fa, List.empty[Json])((list, a) => e(a) :: list).reverse)
    )

}
