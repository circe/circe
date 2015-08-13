package io.circe

import cats.data._

trait Decoders {

  /**
   * @group Decoding
   */
  implicit def decodeNonEmptyList[A: Decoder]: Decoder[NonEmptyList[A]] =
    Decoder.decodeCanBuildFrom[A, List].flatMap { l =>
      Decoder.instance { c =>
        l match {
          case h :: t => Right(NonEmptyList(h, t))
          case Nil => Left(DecodingFailure("[A]NonEmptyList[A]", c.history))
        }
      }
    }.withErrorMessage("[A]NonEmptyList[A]")

  /**
   * @group Disjunction
   */
  def decodeXor[A, B](leftKey: String, rightKey: String)(implicit
    da: Decoder[A],
    db: Decoder[B]
  ): Decoder[Xor[A, B]] =
    Decoder.decodeEither[A, B](
      leftKey,
      rightKey
    ).map(Xor.fromEither).withErrorMessage("[A, B]Xor[A, B]")

  /**
   * @group Disjunction
   */
  def decodeValidated[E, A](failureKey: String, successKey: String)(implicit
    de: Decoder[E],
    da: Decoder[A]
  ): Decoder[Validated[E, A]] =
    Decoder.decodeEither[E, A](
      failureKey,
      successKey
    ).map(Validated.fromEither).withErrorMessage("[E, A]Validated[E, A]")

}
