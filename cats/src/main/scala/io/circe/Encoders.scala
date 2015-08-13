package io.circe

import cats.data._
import cats.std.list._

trait Encoders {

  /**
   * @group Encoding
   */
  implicit def encodeNonEmptyList[A: Encoder]: Encoder[NonEmptyList[A]] =
    new EncoderCompanionOps(Encoder).fromFoldable[NonEmptyList, A]

  /**
   * @group Disjunction
   */
  def encodeXor[A, B](leftKey: String, rightKey: String)(implicit
    ea: Encoder[A],
    eb: Encoder[B]
  ): ObjectEncoder[Xor[A, B]] = ObjectEncoder.instance(
    _.fold(
      a => JsonObject.singleton(leftKey, ea(a)),
      b => JsonObject.singleton(rightKey, eb(b))
    )
  )

  /**
   * @group Disjunction
   */
  def encodeValidated[E, A](failureKey: String, successKey: String)(implicit
    ee: Encoder[E],
    ea: Encoder[A]
  ): ObjectEncoder[Validated[E, A]] = ObjectEncoder.instance(
    _.fold(
      e => JsonObject.singleton(failureKey, ee(e)),
      a => JsonObject.singleton(successKey, ea(a))
    )
  )

}
