package io.circe

import cats.Applicative
import cats.data.ValidatedNel

sealed trait AccumulatingDecoder[A] extends Serializable { self =>
  /**
   * Decode the given hcursor.
   */
  def apply(c: HCursor): AccumulatingDecoder.Result[A]

  /**
   * Map a function over this [[AccumulatingDecoder]].
   */
  def map[B](f: A => B): AccumulatingDecoder[B] = new AccumulatingDecoder[B] {
    def apply(c: HCursor): AccumulatingDecoder.Result[B] = self(c).map(f)
  }
}

object AccumulatingDecoder {
  type Result[A] = ValidatedNel[DecodingFailure, A]

  def fromDecoder[A](implicit decode: Decoder[A]): AccumulatingDecoder[A] =
    new AccumulatingDecoder[A] {
      def apply(c: HCursor): Result[A] = decode.decodeAccumulating(c)
    }

  /*implicit val accumulatingDecoderApplicative: Applicative[AccumulatingDecoder] =
    new Applicative[AccumulatingDecoder] {
      def pure[A](a: A): AccumulatingDecoder[A] = instance(_ => Xor.right(a))
      def flatMap[A, B](fa: AccumulatingDecoder[A])(f: A => Decoder[B]): Decoder[B] = fa.flatMap(f)
    }
  }*/
}
