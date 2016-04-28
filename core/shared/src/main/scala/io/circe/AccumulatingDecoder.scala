package io.circe

import cats.{ ApplicativeError, Semigroup, SemigroupK }
import cats.data.{ NonEmptyList, OneAnd, Validated, ValidatedNel }

sealed trait AccumulatingDecoder[A] extends Serializable { self =>
  /**
   * Decode the given [[HCursor]].
   */
  def apply(c: HCursor): AccumulatingDecoder.Result[A]

  /**
   * Map a function over this [[AccumulatingDecoder]].
   */
  final def map[B](f: A => B): AccumulatingDecoder[B] = new AccumulatingDecoder[B] {
    final def apply(c: HCursor): AccumulatingDecoder.Result[B] = self(c).map(f)
  }

  /**
   * Run two decoders and return their results as a pair.
   */
  final def and[B](other: AccumulatingDecoder[B]): AccumulatingDecoder[(A, B)] = new AccumulatingDecoder[(A, B)] {
    final def apply(c: HCursor): AccumulatingDecoder.Result[(A, B)] = self(c).product(other(c))(
      AccumulatingDecoder.failureNelInstance
    )
  }

  /**
   * Choose the first succeeding decoder.
   */
  final def or[AA >: A](d: => AccumulatingDecoder[AA]): AccumulatingDecoder[AA] = new AccumulatingDecoder[AA] {
    final def apply(c: HCursor): AccumulatingDecoder.Result[AA] = self(c) match {
      case v @ Validated.Valid(_) => v
      case Validated.Invalid(_) => d(c)
    }
  }

  /**
   * Create a new instance that handles any of this instance's errors with the
   * given function.
   */
  final def handleErrorWith(f: NonEmptyList[DecodingFailure] => AccumulatingDecoder[A]): AccumulatingDecoder[A] =
    new AccumulatingDecoder[A] {
      final def apply(c: HCursor): AccumulatingDecoder.Result[A] =
        AccumulatingDecoder.resultInstance.handleErrorWith(self(c))(failure => f(failure)(c))
    }
}

final object AccumulatingDecoder {
  final type Result[A] = ValidatedNel[DecodingFailure, A]

  final val failureNelInstance: Semigroup[NonEmptyList[DecodingFailure]] =
    OneAnd.oneAndSemigroup[List, DecodingFailure](cats.std.list.listInstance)

  final val resultInstance: ApplicativeError[Result, NonEmptyList[DecodingFailure]] =
    Validated.validatedInstances[NonEmptyList[DecodingFailure]](failureNelInstance)

  /**
   * Return an instance for a given type.
   */
  final def apply[A](implicit d: AccumulatingDecoder[A]): AccumulatingDecoder[A] = d

  /**
   * Construct an instance from a function.
   */
  final def instance[A](f: HCursor => Result[A]): AccumulatingDecoder[A] = new AccumulatingDecoder[A] {
    final def apply(c: HCursor): Result[A] = f(c)
  }

  implicit final def fromDecoder[A](implicit decode: Decoder[A]): AccumulatingDecoder[A] = new AccumulatingDecoder[A] {
    final def apply(c: HCursor): Result[A] = decode.decodeAccumulating(c)
  }

  final def failed[A](e: NonEmptyList[DecodingFailure]): AccumulatingDecoder[A] = new AccumulatingDecoder[A] {
    final def apply(c: HCursor): Result[A] = Validated.invalid(e)
  }

  implicit final val accumulatingDecoderInstances: SemigroupK[AccumulatingDecoder] with
    ApplicativeError[AccumulatingDecoder, NonEmptyList[DecodingFailure]] =
    new SemigroupK[AccumulatingDecoder] with ApplicativeError[AccumulatingDecoder, NonEmptyList[DecodingFailure]] {
      final def combineK[A](x: AccumulatingDecoder[A], y: AccumulatingDecoder[A]): AccumulatingDecoder[A] = x.or(y)

      final def pure[A](a: A): AccumulatingDecoder[A] = new AccumulatingDecoder[A] {
        final def apply(c: HCursor): Result[A] = Validated.valid(a)
      }

      override final def map[A, B](fa: AccumulatingDecoder[A])(f: A => B): AccumulatingDecoder[B] = fa.map(f)

      final def ap[A, B](f: AccumulatingDecoder[A => B])(fa: AccumulatingDecoder[A]): AccumulatingDecoder[B] =
        new AccumulatingDecoder[B] {
          final def apply(c: HCursor): Result[B] = resultInstance.ap(f(c))(fa(c))
        }

      override final def product[A, B](
        fa: AccumulatingDecoder[A],
        fb: AccumulatingDecoder[B]
      ): AccumulatingDecoder[(A, B)] = fa.and(fb)

      final def raiseError[A](e: NonEmptyList[DecodingFailure]): AccumulatingDecoder[A] = AccumulatingDecoder.failed(e)

      final def handleErrorWith[A](fa: AccumulatingDecoder[A])(
        f: NonEmptyList[DecodingFailure] => AccumulatingDecoder[A]
      ): AccumulatingDecoder[A] = fa.handleErrorWith(f)
    }
}
