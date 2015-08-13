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
          case h :: t => Xor.right(NonEmptyList(h, t))
          case Nil => Xor.left(DecodingFailure("[A]NonEmptyList[A]", c.history))
        }
      }
    }.withErrorMessage("[A]NonEmptyList[A]")

  /**
   * @group Disjunction
   */
  def decodeValidated[E, A](failureKey: String, successKey: String)(implicit
    de: Decoder[E],
    da: Decoder[A]
  ): Decoder[Validated[E, A]] =
    Decoder.decodeXor[E, A](
      failureKey,
      successKey
    ).map(_.toValidated).withErrorMessage("[E, A]Validated[E, A]")

}
