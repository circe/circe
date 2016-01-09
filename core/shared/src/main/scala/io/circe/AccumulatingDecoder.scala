package io.circe

import cats.{ Applicative, Semigroup }
import cats.data.{ NonEmptyList, OneAnd, Validated, ValidatedNel }

sealed trait AccumulatingDecoder[A] extends Serializable { self =>
  /**
   * Decode the given hcursor.
   */
  def apply(c: HCursor): AccumulatingDecoder.Result[A]

  /**
   * Map a function over this [[AccumulatingDecoder]].
   */
  final def map[B](f: A => B): AccumulatingDecoder[B] = new AccumulatingDecoder[B] {
    final def apply(c: HCursor): AccumulatingDecoder.Result[B] = self(c).map(f)
  }

  final def ap[B](f: AccumulatingDecoder[A => B]): AccumulatingDecoder[B] =
    new AccumulatingDecoder[B] {
      final def apply(c: HCursor): AccumulatingDecoder.Result[B] = self(c).ap(f(c))(
        AccumulatingDecoder.failureNelInstance
      )
    }
}

final object AccumulatingDecoder {
  final type Result[A] = ValidatedNel[DecodingFailure, A]

  final val failureNelInstance: Semigroup[NonEmptyList[DecodingFailure]] =
    OneAnd.oneAndSemigroup[List, DecodingFailure](cats.std.list.listInstance)

  final val resultInstance: Applicative[({ type L[x] = ValidatedNel[DecodingFailure, x] })#L] =
    Validated.validatedInstances[NonEmptyList[DecodingFailure]](failureNelInstance)

  /**
   * Return an instance for a given type.
   */
  final def apply[A](implicit d: AccumulatingDecoder[A]): AccumulatingDecoder[A] = d

  implicit final def fromDecoder[A](implicit decode: Decoder[A]): AccumulatingDecoder[A] =
    new AccumulatingDecoder[A] {
      final def apply(c: HCursor): Result[A] = decode.decodeAccumulating(c)
    }

  implicit final val accumulatingDecoderInstance: Applicative[AccumulatingDecoder] =
    new Applicative[AccumulatingDecoder] {
      final def pure[A](a: A): AccumulatingDecoder[A] = new AccumulatingDecoder[A] {
        final def apply(c: HCursor): AccumulatingDecoder.Result[A] = Validated.valid(a)
      }

      override final def map[A, B](fa: AccumulatingDecoder[A])(f: A => B): AccumulatingDecoder[B] =
        fa.map(f)

      final def ap[A, B](fa: AccumulatingDecoder[A])(
        f: AccumulatingDecoder[A => B]
      ): AccumulatingDecoder[B] = fa.ap(f)

      final def product[A, B](
        fa: AccumulatingDecoder[A],
        fb: AccumulatingDecoder[B]
      ): AccumulatingDecoder[(A, B)] = new AccumulatingDecoder[(A, B)] {
        final def apply(c: HCursor): AccumulatingDecoder.Result[(A, B)] =
          fb(c).ap(fa(c).map(a => (b: B) => (a, b)))(failureNelInstance)
      }
    }
}
