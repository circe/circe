/*
 * Copyright 2024 circe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.circe

import cats.ApplicativeError
import cats.Defer
import cats.Eval
import cats.MonadError
import cats.SemigroupK
import cats.data.Chain
import cats.data.Kleisli
import cats.data.NonEmptyChain
import cats.data.NonEmptyList
import cats.data.NonEmptyMap
import cats.data.NonEmptySeq
import cats.data.NonEmptySet
import cats.data.NonEmptyVector
import cats.data.StateT
import cats.data.Validated
import cats.data.Validated.Invalid
import cats.data.Validated.Valid
import cats.data.ValidatedNel
import cats.instances.either.catsStdInstancesForEither
import cats.instances.either.catsStdSemigroupKForEither
import cats.kernel.Order
import cats.syntax.either._
import io.circe.DecodingFailure.Reason.MissingField
import io.circe.DecodingFailure.Reason.WrongTypeExpectation
import io.circe.`export`.Exported

import java.io.Serializable
import java.net.URI
import java.net.URISyntaxException
import java.time.DateTimeException
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.MonthDay
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.Period
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.UUID
import scala.annotation.tailrec
import scala.collection.immutable.Set
import scala.collection.immutable.SortedMap
import scala.collection.immutable.SortedSet
import scala.collection.immutable.{ Map => ImmutableMap }
import scala.collection.mutable.Builder
import scala.util.Failure
import scala.util.Success
import scala.util.Try

/**
 * A type class that provides a way to produce a value of type `A` from a [[Json]] value.
 */
trait Decoder[A] extends Serializable { self =>

  /**
   * Decode the given [[HCursor]].
   */
  def apply(c: HCursor): Decoder.Result[A]

  def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[A] = apply(c) match {
    case Right(a) => Validated.valid(a)
    case Left(e)  => Validated.invalidNel(e)
  }

  private[this] def cursorToDecodingFailure(cursor: ACursor): DecodingFailure = {
    val reason: Eval[DecodingFailure.Reason] =
      Eval.later(
        cursor match {
          case cursor: FailedCursor if cursor.missingField =>
            DecodingFailure.Reason.MissingField
          case _ =>
            val field: String = cursor.pathString.replaceFirst("^\\.", "")
            DecodingFailure.Reason.CustomReason(s"Couldn't decode $field")
        }
      )

    DecodingFailure(reason, Some(cursor.pathToRoot), Eval.later(cursor.history))
  }

  /**
   * Decode the given [[ACursor]].
   *
   * Note that if you override the default implementation, you should also be
   * sure to override `tryDecodeAccumulating` in order for fail-fast and
   * accumulating decoding to be consistent.
   */
  def tryDecode(c: ACursor): Decoder.Result[A] = c match {
    case hc: HCursor => apply(hc)
    case _ =>
      Left(cursorToDecodingFailure(c))
  }

  def tryDecodeAccumulating(c: ACursor): Decoder.AccumulatingResult[A] = c match {
    case hc: HCursor => decodeAccumulating(hc)
    case _ =>
      Validated.invalidNel(
        cursorToDecodingFailure(c)
      )
  }

  /**
   * Decode the given [[Json]] value.
   */
  final def decodeJson(j: Json): Decoder.Result[A] = apply(HCursor.fromJson(j))

  /**
   * Map a function over this [[Decoder]].
   */
  final def map[B](f: A => B): Decoder[B] = new Decoder[B] {
    final def apply(c: HCursor): Decoder.Result[B] = tryDecode(c)
    override def tryDecode(c: ACursor): Decoder.Result[B] = self.tryDecode(c) match {
      case Right(a)    => Right(f(a))
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[B]]
    }
    override final def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[B] =
      tryDecodeAccumulating(c)

    override final def tryDecodeAccumulating(c: ACursor): Decoder.AccumulatingResult[B] =
      self.tryDecodeAccumulating(c).map(f)
  }

  /**
   * Monadically bind a function over this [[Decoder]].
   */
  final def flatMap[B](f: A => Decoder[B]): Decoder[B] = new Decoder[B] {
    final def apply(c: HCursor): Decoder.Result[B] = self(c) match {
      case Right(a)    => f(a)(c)
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[B]]
    }

    override def tryDecode(c: ACursor): Decoder.Result[B] = self.tryDecode(c) match {
      case Right(a)    => f(a).tryDecode(c)
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[B]]
    }

    override final def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[B] =
      self.decodeAccumulating(c).andThen(result => f(result).decodeAccumulating(c))

    override final def tryDecodeAccumulating(c: ACursor): Decoder.AccumulatingResult[B] =
      self.tryDecodeAccumulating(c).andThen(result => f(result).tryDecodeAccumulating(c))
  }

  /**
   * Create a new instance that handles any of this instance's errors with the
   * given function.
   *
   * Note that in the case of accumulating decoding, only the first error will
   * be used in recovery.
   */
  final def handleErrorWith(f: DecodingFailure => Decoder[A]): Decoder[A] = new Decoder[A] {
    final def apply(c: HCursor): Decoder.Result[A] = tryDecode(c)
    override final def tryDecode(c: ACursor): Decoder.Result[A] =
      Decoder.resultInstance.handleErrorWith(self.tryDecode(c))(failure => f(failure).tryDecode(c))

    override final def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[A] =
      tryDecodeAccumulating(c)
    override final def tryDecodeAccumulating(c: ACursor): Decoder.AccumulatingResult[A] =
      Decoder.accumulatingResultInstance.handleErrorWith(self.tryDecodeAccumulating(c))(failures =>
        f(failures.head).tryDecodeAccumulating(c)
      )
  }

  /**
   * Build a new instance with the specified error message.
   */
  final def withErrorMessage(message: String): Decoder[A] = new Decoder[A] {
    final def apply(c: HCursor): Decoder.Result[A] = self(c) match {
      case r @ Right(_) => r
      case Left(e)      => Left(e.withMessage(message))
    }

    override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[A] =
      self.decodeAccumulating(c).leftMap(_.map(_.withMessage(message)))
  }

  /**
   * Build a new instance that fails if the condition does not hold for the
   * result.
   *
   * Note that in the case of chained calls to this method, only the first
   * failure will be returned.
   */
  final def ensure(pred: A => Boolean, message: => String): Decoder[A] = new Decoder[A] {
    final def apply(c: HCursor): Decoder.Result[A] = self(c) match {
      case r @ Right(a) => if (pred(a)) r else Left(DecodingFailure(message, c.history))
      case l @ Left(_)  => l
    }

    override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[A] = self.decodeAccumulating(c) match {
      case v @ Valid(a)   => if (pred(a)) v else Validated.invalidNel(DecodingFailure(message, c.history))
      case i @ Invalid(_) => i
    }
  }

  /**
   * Build a new instance that fails with one or more errors if the condition
   * does not hold for the result.
   *
   * If the result of the function applied to the decoded value is the empty
   * list, the new decoder will succeed with that value.
   */
  final def ensure(errors: A => List[String]): Decoder[A] = new Decoder[A] {
    final def apply(c: HCursor): Decoder.Result[A] = self(c) match {
      case r @ Right(a) =>
        errors(a) match {
          case Nil          => r
          case message :: _ => Left(DecodingFailure(message, c.history))
        }
      case l @ Left(_) => l
    }

    override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[A] = self.decodeAccumulating(c) match {
      case v @ Valid(a) =>
        errors(a).map(DecodingFailure(_, c.history)) match {
          case Nil    => v
          case h :: t => Validated.invalid(NonEmptyList(h, t))
        }
      case i @ Invalid(_) => i
    }
  }

  /**
   * Build a new instance that fails if the condition does not hold for the
   * input.
   */
  final def validate(errors: HCursor => List[String]): Decoder[A] = new Decoder[A] {
    final def apply(c: HCursor): Decoder.Result[A] =
      errors(c).headOption.map { message =>
        Left(DecodingFailure(message, c.history))
      }.getOrElse(self(c))

    override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[A] = {
      val underlyingResult = self.decodeAccumulating(c)
      errors(c).map(DecodingFailure(_, c.history)) match {
        case Nil =>
          underlyingResult
        case h :: t =>
          val errorsNel = NonEmptyList(h, t)
          Validated.invalid(underlyingResult.fold(errorsNel ::: _, _ => errorsNel))
      }
    }
  }

  /**
   * Build a new instance that fails if the condition does not hold for the
   * input.
   */
  final def validate(pred: HCursor => Boolean, message: => String): Decoder[A] = validate { c =>
    if (pred(c)) Nil
    else message :: Nil
  }

  /**
   * Convert to a Kleisli arrow.
   */
  final def kleisli: Kleisli[Decoder.Result, HCursor, A] =
    Kleisli[Decoder.Result, HCursor, A](apply(_))

  /**
   * Run two decoders and return their results as a pair.
   */
  final def product[B](fb: Decoder[B]): Decoder[(A, B)] = new Decoder[(A, B)] {
    final def apply(c: HCursor): Decoder.Result[(A, B)] = Decoder.resultInstance.product(self(c), fb(c))
    override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[(A, B)] =
      Decoder.accumulatingResultInstance.product(self.decodeAccumulating(c), fb.decodeAccumulating(c))
  }

  /**
   * Choose the first succeeding decoder.
   */
  final def or[AA >: A](d: => Decoder[AA]): Decoder[AA] = new Decoder[AA] {
    final def apply(c: HCursor): Decoder.Result[AA] = tryDecode(c)
    override def tryDecode(c: ACursor): Decoder.Result[AA] = self.tryDecode(c) match {
      case r @ Right(_) => r
      case Left(_)      => d.tryDecode(c)
    }
    override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[AA] =
      tryDecodeAccumulating(c)
    override def tryDecodeAccumulating(c: ACursor): Decoder.AccumulatingResult[AA] =
      self.tryDecodeAccumulating(c) match {
        case r @ Valid(_) => r
        case Invalid(_)   => d.tryDecodeAccumulating(c)
      }
  }

  /**
   * Choose the first succeeding decoder, wrapping the result in a disjunction.
   */
  final def either[B](decodeB: Decoder[B]): Decoder[Either[A, B]] = new Decoder[Either[A, B]] {
    final def apply(c: HCursor): Decoder.Result[Either[A, B]] = tryDecode(c)
    override def tryDecode(c: ACursor): Decoder.Result[Either[A, B]] = self.tryDecode(c) match {
      case Right(v) => Right(Left(v))
      case Left(_) =>
        decodeB.tryDecode(c) match {
          case Right(v)    => Right(Right(v))
          case l @ Left(_) => l.asInstanceOf[Decoder.Result[Either[A, B]]]
        }
    }
    override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[Either[A, B]] = tryDecodeAccumulating(c)
    override def tryDecodeAccumulating(c: ACursor): Decoder.AccumulatingResult[Either[A, B]] =
      self.tryDecodeAccumulating(c) match {
        case Valid(v) => Valid(Left(v))
        case Invalid(_) =>
          decodeB.tryDecodeAccumulating(c) match {
            case Valid(v)       => Valid(Right(v))
            case l @ Invalid(_) => l.asInstanceOf[Decoder.AccumulatingResult[Either[A, B]]]
          }
      }
  }

  /**
   * Create a new decoder that performs some operation on the incoming JSON before decoding.
   */
  final def prepare(f: ACursor => ACursor): Decoder[A] = new Decoder[A] {
    final def apply(c: HCursor): Decoder.Result[A] = tryDecode(c)
    override def tryDecode(c: ACursor): Decoder.Result[A] = self.tryDecode(f(c))
    override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[A] =
      tryDecodeAccumulating(c)
    override def tryDecodeAccumulating(c: ACursor): Decoder.AccumulatingResult[A] =
      self.tryDecodeAccumulating(f(c))
  }

  /**
   * Create a new decoder that attempts to navigate to the specified field
   * before decoding.
   */
  final def at(field: String): Decoder[A] = new Decoder[A] {
    private[this] val f: String = field
    final def apply(c: HCursor): Decoder.Result[A] = tryDecode(c)
    override def tryDecode(c: ACursor): Decoder.Result[A] = self.tryDecode(c.downField(f))
    override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[A] =
      tryDecodeAccumulating(c)
    override def tryDecodeAccumulating(c: ACursor): Decoder.AccumulatingResult[A] =
      self.tryDecodeAccumulating(c.downField(f))
  }

  /**
   * Create a new decoder that performs some operation on the result if this one succeeds.
   *
   * @param f a function returning either a value or an error message
   */
  final def emap[B](f: A => Either[String, B]): Decoder[B] = new Decoder[B] {
    final def apply(c: HCursor): Decoder.Result[B] = tryDecode(c)

    override def tryDecode(c: ACursor): Decoder.Result[B] =
      self.tryDecode(c) match {
        case Right(a) =>
          f(a) match {
            case r @ Right(_)  => r.asInstanceOf[Decoder.Result[B]]
            case Left(message) => Left(DecodingFailure(message, c.history))
          }
        case l @ Left(_) => l.asInstanceOf[Decoder.Result[B]]
      }

    override final def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[B] =
      tryDecodeAccumulating(c)

    override final def tryDecodeAccumulating(c: ACursor): Decoder.AccumulatingResult[B] =
      self.tryDecodeAccumulating(c) match {
        case Valid(a) =>
          f(a) match {
            case Right(b)      => Validated.valid(b)
            case Left(message) => Validated.invalidNel(DecodingFailure(message, c.history))
          }
        case l @ Invalid(_) => l.asInstanceOf[Decoder.AccumulatingResult[B]]
      }
  }

  /**
   * Create a new decoder that performs some operation on the result if this one succeeds.
   *
   * @param f a function returning either a value or an error message
   */
  final def emapTry[B](f: A => Try[B]): Decoder[B] = new Decoder[B] {
    final def apply(c: HCursor): Decoder.Result[B] = tryDecode(c)

    override def tryDecode(c: ACursor): Decoder.Result[B] =
      self.tryDecode(c) match {
        case Right(a) =>
          f(a) match {
            case Success(b) => Right(b)
            case Failure(t) => Left(DecodingFailure.fromThrowable(t, c.history))
          }
        case l @ Left(_) => l.asInstanceOf[Decoder.Result[B]]
      }

    override final def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[B] =
      tryDecodeAccumulating(c)

    override final def tryDecodeAccumulating(c: ACursor): Decoder.AccumulatingResult[B] =
      self.tryDecodeAccumulating(c) match {
        case Valid(a) =>
          f(a) match {
            case Success(b) => Validated.valid(b)
            case Failure(t) => Validated.invalidNel(DecodingFailure.fromThrowable(t, c.history))
          }
        case l @ Invalid(_) => l.asInstanceOf[Decoder.AccumulatingResult[B]]
      }
  }
}

/**
 * Utilities and instances for [[Decoder]].
 *
 * @groupname Aliases Type aliases
 * @groupprio Aliases 0
 *
 * @groupname Utilities Defining decoders
 * @groupprio Utilities 1
 *
 * @groupname Decoding General decoder instances
 * @groupprio Decoding 2
 *
 * @groupname Collection Collection instances
 * @groupprio Collection 3
 *
 * @groupname Disjunction Disjunction instances
 * @groupdesc Disjunction Instance creation methods for disjunction-like types. Note that these
 * instances are not implicit, since they require non-obvious decisions about the names of the
 * discriminators. If you want instances for these types you can include the following import in
 * your program:
 * {{{
 *   import io.circe.disjunctionCodecs._
 * }}}
 * @groupprio Disjunction 4
 *
 * @groupname Instances Type class instances
 * @groupprio Instances 5
 *
 * @groupname Tuple Tuple instances
 * @groupprio Tuple 6
 *
 * @groupname Product Case class and other product instances
 * @groupprio Product 7
 *
 * @groupname Time Java date and time instances
 * @groupprio Time 8
 *
 * @groupname Literal Literal type instances
 * @groupprio Literal 9
 *
 * @groupname Prioritization Instance prioritization
 * @groupprio Prioritization 10
 *
 * @author Travis Brown
 */
object Decoder
    extends DecoderDerivation
    with CollectionDecoders
    with TupleDecoders
    with ProductDecoders
    with LiteralDecoders
    with EnumerationDecoders
    with LowPriorityDecoders {

  /**
   * @group Aliases
   */
  final type Result[A] = Either[DecodingFailure, A]

  /**
   * @group Aliases
   */
  final type AccumulatingResult[A] = ValidatedNel[DecodingFailure, A]

  /**
   * @group Instances
   */
  final val resultInstance: MonadError[Result, DecodingFailure] = catsStdInstancesForEither[DecodingFailure]

  /**
   * @group Instances
   */
  final val accumulatingResultInstance: ApplicativeError[AccumulatingResult, NonEmptyList[DecodingFailure]] =
    Validated.catsDataApplicativeErrorForValidated[NonEmptyList[DecodingFailure]](
      NonEmptyList.catsDataSemigroupForNonEmptyList[DecodingFailure]
    )

  private[circe] val resultSemigroupK: SemigroupK[Result] = catsStdSemigroupKForEither[DecodingFailure]

  private[this] abstract class DecoderWithFailure[A](name: String) extends Decoder[A] {
    final def fail(c: HCursor): Result[A] = Left(DecodingFailure(name, c.history))
  }

  /**
   * Return an instance for a given type.
   *
   * @group Utilities
   */
  final def apply[A](implicit instance: Decoder[A]): Decoder[A] = instance

  /**
   * Create a decoder that always returns a single value, useful with some `flatMap` situations.
   *
   * @group Utilities
   */
  final def const[A](a: A): Decoder[A] = new Decoder[A] {
    final def apply(c: HCursor): Result[A] = Right(a)
    final override def decodeAccumulating(c: HCursor): AccumulatingResult[A] =
      Validated.valid(a)
  }

  /**
   * Construct an instance from a function.
   *
   * @group Utilities
   */
  final def instance[A](f: HCursor => Result[A]): Decoder[A] = new Decoder[A] {
    final def apply(c: HCursor): Result[A] = f(c)
  }

  /**
   * Construct an instance from a [[cats.data.StateT]] value.
   *
   * @group Utilities
   */
  def fromState[A](s: StateT[Result, ACursor, A]): Decoder[A] = new Decoder[A] {
    final def apply(c: HCursor): Result[A] = s.runA(c)
  }

  /**
   * This is for easier interop with code that already returns [[scala.util.Try]]. You should
   * prefer `instance` for any new code.
   *
   * @group Utilities
   */
  final def instanceTry[A](f: HCursor => Try[A]): Decoder[A] = new Decoder[A] {
    final def apply(c: HCursor): Result[A] = f(c) match {
      case Success(a) => Right(a)
      case Failure(t) => Left(DecodingFailure.fromThrowable(t, c.history))
    }
  }

  /**
   * Construct an instance from a function that may reattempt on failure.
   *
   * @group Utilities
   */
  final def withReattempt[A](f: ACursor => Result[A]): Decoder[A] = new Decoder[A] {
    final def apply(c: HCursor): Result[A] = tryDecode(c)

    override def tryDecode(c: ACursor): Decoder.Result[A] = f(c)

    override def decodeAccumulating(c: HCursor): AccumulatingResult[A] = tryDecodeAccumulating(c)

    override def tryDecodeAccumulating(c: ACursor): AccumulatingResult[A] = f(c) match {
      case Right(v) => Validated.valid(v)
      case Left(e)  => Validated.invalidNel(e)
    }
  }

  /**
   * Construct an instance that always fails with the given [[DecodingFailure]].
   *
   * @group Utilities
   */
  final def failed[A](failure: DecodingFailure): Decoder[A] = new Decoder[A] {
    final def apply(c: HCursor): Result[A] = Left(failure)
    override final def decodeAccumulating(c: HCursor): AccumulatingResult[A] =
      Validated.invalidNel(failure)
  }

  /**
   * Construct an instance that always fails with the given error message.
   *
   * @group Utilities
   */
  final def failedWithMessage[A](message: String): Decoder[A] = new Decoder[A] {
    final def apply(c: HCursor): Result[A] = Left(DecodingFailure(message, c.history))
    override final def decodeAccumulating(c: HCursor): AccumulatingResult[A] =
      Validated.invalidNel(DecodingFailure(message, c.history))
  }

  /**
   * Create a `Decoder` which assumes one already exists
   *
   * Certain recursive data structures (particularly when generic) greatly benefit from
   * being able to be written this way. See `cats.Defer`
   *
   * Note: while a `Decoder` written using `Decoder.recursive` can prevent unneeded creation of
   * `Decoder` instances when recursing, the resulting `Decoder` cannot be guaranteed to be stack-safe.
   *
   * @group Utilities
   */
  final def recursive[A](fn: Decoder[A] => Decoder[A]): Decoder[A] = Defer[Decoder].fix(fn)

  /**
   * @group Decoding
   */
  implicit final val decodeHCursor: Decoder[HCursor] = new Decoder[HCursor] {
    final def apply(c: HCursor): Result[HCursor] = Right(c)
  }

  /**
   * @group Decoding
   */
  implicit final val decodeJson: Decoder[Json] = new Decoder[Json] {
    final def apply(c: HCursor): Result[Json] = Right(c.value)
  }

  /**
   * @group Decoding
   */
  implicit final val decodeJsonObject: Decoder[JsonObject] = new Decoder[JsonObject] {
    final def apply(c: HCursor): Result[JsonObject] = c.value.asObject match {
      case Some(v) => Right(v)
      case None    => Left(DecodingFailure(WrongTypeExpectation("object", c.value), c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit final val decodeJsonNumber: Decoder[JsonNumber] = new Decoder[JsonNumber] {
    final def apply(c: HCursor): Result[JsonNumber] = c.value.asNumber match {
      case Some(v) => Right(v)
      case None    => Left(DecodingFailure(WrongTypeExpectation("number", c.value), c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit final val decodeString: Decoder[String] = new Decoder[String] {
    final def apply(c: HCursor): Result[String] = c.value match {
      case Json.JString(string) => Right(string)
      case json                 => Left(DecodingFailure(WrongTypeExpectation("string", json), c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit final val decodeUnit: Decoder[Unit] = new Decoder[Unit] {
    final def apply(c: HCursor): Result[Unit] = c.value match {
      case Json.JObject(obj) if obj.isEmpty => Right(())
      case Json.JArray(arr) if arr.isEmpty  => Right(())
      case other if other.isNull            => Right(())
      case json =>
        Left(DecodingFailure(WrongTypeExpectation("'null' or '[]' or '{}'", json), c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit final val decodeBoolean: Decoder[Boolean] = new Decoder[Boolean] {
    final def apply(c: HCursor): Result[Boolean] = c.value match {
      case Json.JBoolean(b) => Right(b)
      case json =>
        Left(DecodingFailure(WrongTypeExpectation("'true' or 'false'", json), c.history))
    }
  }

  /**
   * Decode a JSON value into a `java.lang.Boolean`.
   *
   * @group Decoding
   */
  implicit final lazy val decodeJavaBoolean: Decoder[java.lang.Boolean] = decodeBoolean.map(java.lang.Boolean.valueOf)

  /**
   * @group Decoding
   */
  implicit final val decodeChar: Decoder[Char] = new Decoder[Char] {
    final def apply(c: HCursor): Result[Char] = c.value match {
      case Json.JString(string) if string.length == 1 => Right(string.charAt(0))
      case json =>
        Left(DecodingFailure(WrongTypeExpectation("character", json), c.history))
    }
  }

  /**
   * Decode a JSON value into a `java.lang.Character`.
   *
   * @group Decoding
   */
  implicit final lazy val decodeJavaCharacter: Decoder[java.lang.Character] =
    decodeChar.map(java.lang.Character.valueOf)

  /**
   * Decode a JSON value into a [[scala.Float]].
   *
   * See [[decodeDouble]] for discussion of the approach taken for floating-point decoding.
   *
   * @group Decoding
   */
  implicit final val decodeFloat: Decoder[Float] = new DecoderWithFailure[Float]("Float") {
    final def apply(c: HCursor): Result[Float] = c.value match {
      case Json.JNumber(number) => Right(number.toFloat)
      case Json.JString(string) =>
        JsonNumber.fromString(string).map(_.toFloat) match {
          case Some(v) => Right(v)
          case None    => fail(c)
        }
      case other if other.isNull => Right(Float.NaN)
      case _                     => fail(c)
    }
  }

  /**
   * Decode a JSON value into a `java.lang.Float`.
   *
   * @group Decoding
   */
  implicit final lazy val decodeJavaFloat: Decoder[java.lang.Float] = decodeFloat.map(java.lang.Float.valueOf)

  /**
   * Decode a JSON value into a [[scala.Double]].
   *
   * Unlike the integral decoders provided here, this decoder will accept values that are too large
   * to be represented and will return them as `PositiveInfinity` or `NegativeInfinity`, and it may
   * lose precision.
   *
   * @group Decoding
   */
  implicit final val decodeDouble: Decoder[Double] = new DecoderWithFailure[Double]("Double") {
    final def apply(c: HCursor): Result[Double] = c.value match {
      case Json.JNumber(number) => Right(number.toDouble)
      case Json.JString(string) =>
        JsonNumber.fromString(string).map(_.toDouble) match {
          case Some(v) => Right(v)
          case None    => fail(c)
        }
      case other if other.isNull => Right(Double.NaN)
      case _                     => fail(c)
    }
  }

  /**
   * Decode a JSON value into a `java.lang.Double`.
   *
   * @group Decoding
   */
  implicit final lazy val decodeJavaDouble: Decoder[java.lang.Double] = decodeDouble.map(java.lang.Double.valueOf)

  /**
   * Decode a JSON value into a [[scala.Byte]].
   *
   * See [[decodeLong]] for discussion of the approach taken for integral decoding.
   *
   * @group Decoding
   */
  implicit final val decodeByte: Decoder[Byte] = new DecoderWithFailure[Byte]("Byte") {
    final def apply(c: HCursor): Result[Byte] = c.value match {
      case Json.JNumber(number) =>
        number.toByte match {
          case Some(v) => Right(v)
          case None    => fail(c)
        }
      case Json.JString(string) =>
        JsonNumber.fromString(string).flatMap(_.toByte) match {
          case Some(value) => Right(value)
          case None        => fail(c)
        }
      case _ => fail(c)
    }
  }

  /**
   * Decode a JSON value into a `java.lang.Byte`.
   *
   * @group Decoding
   */
  implicit final lazy val decodeJavaByte: Decoder[java.lang.Byte] = decodeByte.map(java.lang.Byte.valueOf)

  /**
   * Decode a JSON value into a [[scala.Short]].
   *
   * See [[decodeLong]] for discussion of the approach taken for integral decoding.
   *
   * @group Decoding
   */
  implicit final val decodeShort: Decoder[Short] = new DecoderWithFailure[Short]("Short") {
    final def apply(c: HCursor): Result[Short] = c.value match {
      case Json.JNumber(number) =>
        number.toShort match {
          case Some(v) => Right(v)
          case None    => fail(c)
        }
      case Json.JString(string) =>
        JsonNumber.fromString(string).flatMap(_.toShort) match {
          case Some(value) => Right(value)
          case None        => fail(c)
        }
      case _ => fail(c)
    }
  }

  /**
   * Decode a JSON value into a `java.lang.Short`.
   *
   * @group Decoding
   */
  implicit final lazy val decodeJavaShort: Decoder[java.lang.Short] = decodeShort.map(java.lang.Short.valueOf)

  /**
   * Decode a JSON value into a [[scala.Int]].
   *
   * See [[decodeLong]] for discussion of the approach taken for integral decoding.
   *
   * @group Decoding
   */
  implicit final val decodeInt: Decoder[Int] = new DecoderWithFailure[Int]("Int") {
    final def apply(c: HCursor): Result[Int] = c.value match {
      case Json.JNumber(number) =>
        number.toInt match {
          case Some(v) => Right(v)
          case None    => fail(c)
        }
      case Json.JString(string) =>
        JsonNumber.fromString(string).flatMap(_.toInt) match {
          case Some(value) => Right(value)
          case None        => fail(c)
        }
      case _ => fail(c)
    }
  }

  /**
   * Decode a JSON value into a `java.lang.Integer`.
   *
   * @group Decoding
   */
  implicit final lazy val decodeJavaInteger: Decoder[java.lang.Integer] = decodeInt.map(java.lang.Integer.valueOf)

  /**
   * Decode a JSON value into a [[scala.Long]].
   *
   * Decoding will fail if the value doesn't represent a whole number within the range of the target
   * type (although it can have a decimal part: e.g. `10.0` will be successfully decoded, but
   * `10.01` will not). If the value is a JSON string, the decoder will attempt to parse it as a
   * number.
   *
   * @group Decoding
   */
  implicit final val decodeLong: Decoder[Long] = new DecoderWithFailure[Long]("Long") {
    final def apply(c: HCursor): Result[Long] = c.value match {
      case Json.JNumber(number) =>
        number.toLong match {
          case Some(v) => Right(v)
          case None    => fail(c)
        }
      case Json.JString(string) =>
        JsonNumber.fromString(string).flatMap(_.toLong) match {
          case Some(value) => Right(value)
          case None        => fail(c)
        }
      case _ => fail(c)
    }
  }

  /**
   * Decode a JSON value into a `java.lang.Long`.
   *
   * @group Decoding
   */
  implicit final lazy val decodeJavaLong: Decoder[java.lang.Long] = decodeLong.map(java.lang.Long.valueOf)

  /**
   * Decode a JSON value into a [[scala.math.BigInt]].
   *
   * Note that decoding will fail if the number has a large number of digits (the limit is currently
   * `1 << 18`, or around a quarter million). Larger numbers can be decoded by mapping over a
   * [[scala.math.BigDecimal]], but be aware that the conversion to the integral form can be
   * computationally expensive.
   *
   * @group Decoding
   */
  implicit final val decodeBigInt: Decoder[BigInt] = new DecoderWithFailure[BigInt]("BigInt") {
    final def apply(c: HCursor): Result[BigInt] = c.value match {
      case Json.JNumber(number) =>
        number.toBigInt match {
          case Some(v) => Right(v)
          case None    => fail(c)
        }
      case Json.JString(string) =>
        JsonNumber.fromString(string).flatMap(_.toBigInt) match {
          case Some(value) => Right(value)
          case None        => fail(c)
        }
      case _ => fail(c)
    }
  }

  /**
   * Decode a JSON value into a `java.math.BigInteger`.
   *
   * @group Decoding
   */
  implicit final lazy val decodeJavaBigInteger: Decoder[java.math.BigInteger] = decodeBigInt.map(_.bigInteger)

  /**
   * Decode a JSON value into a [[scala.math.BigDecimal]].
   *
   * Note that decoding will fail on some very large values that could in principle be represented
   * as `BigDecimal`s (specifically if the `scale` is out of the range of `scala.Int` when the
   * `unscaledValue` is adjusted to have no trailing zeros). These large values can, however, be
   * round-tripped through `JsonNumber`, so you may wish to use [[decodeJsonNumber]] in these cases.
   *
   * Also note that because `scala.scalajs.js.JSON` parses JSON numbers into a floating point
   * representation, decoding a JSON number into a `BigDecimal` on Scala.js may lose precision.
   *
   * @group Decoding
   */
  implicit final val decodeBigDecimal: Decoder[BigDecimal] = new DecoderWithFailure[BigDecimal]("BigDecimal") {
    final def apply(c: HCursor): Result[BigDecimal] = c.value match {
      case Json.JNumber(number) =>
        number.toBigDecimal match {
          case Some(v) => Right(v)
          case None    => fail(c)
        }
      case Json.JString(string) =>
        JsonNumber.fromString(string).flatMap(_.toBigDecimal) match {
          case Some(value) => Right(value)
          case None        => fail(c)
        }
      case _ => fail(c)
    }
  }

  /**
   * Decode a JSON value into a `java.math.BigDecimal`.
   *
   * @group Decoding
   */
  implicit final lazy val decodeJavaBigDecimal: Decoder[java.math.BigDecimal] = decodeBigDecimal.map(_.bigDecimal)

  /**
   * @group Decoding
   */
  implicit final lazy val decodeUUID: Decoder[UUID] = new Decoder[UUID] {

    final def apply(c: HCursor): Result[UUID] = c.value match {
      case Json.JString(string) if string.length == 36 =>
        try Right(UUID.fromString(string))
        catch {
          case _: IllegalArgumentException =>
            Left(DecodingFailure("Couldn't decode a valid UUID", c.history))
        }
      case json => Left(DecodingFailure(WrongTypeExpectation("string", json), c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit final lazy val decodeURI: Decoder[URI] = new Decoder[URI] {

    final def apply(c: HCursor): Result[URI] = c.value match {
      case Json.JString(string) =>
        try Right(new URI(string))
        catch {
          case _: URISyntaxException =>
            Left(DecodingFailure("String could not be parsed as a URI reference, it violates RFC 2396.", c.history))
          case _: NullPointerException =>
            Left(DecodingFailure("String is null.", c.history))
        }
      case json => Left(DecodingFailure(WrongTypeExpectation("string", json), c.history))
    }
  }

  private[this] final val rightNone: Either[DecodingFailure, None.type] = Right(None)
  private[this] final val validNone: ValidatedNel[DecodingFailure, None.type] = Validated.valid(None)

  private[this] final val rightNull: Either[DecodingFailure, NullOr.Null.type] = Right(NullOr.Null)
  private[this] final val validNull: ValidatedNel[DecodingFailure, NullOr.Null.type] = Validated.valid(NullOr.Null)

  private[this] final val rightSomeNull: Either[DecodingFailure, Some[NullOr.Null.type]] = Right(Some(NullOr.Null))
  private[this] final val validSomeNull: ValidatedNel[DecodingFailure, Some[NullOr.Null.type]] =
    Validated.valid(Some(NullOr.Null))

  private[circe] final val keyMissingNone: Decoder.Result[None.type] = Right(None)
  private[circe] final val keyMissingNoneAccumulating: AccumulatingResult[None.type] =
    Validated.valid(None)

  /**
   * A decoder for `Option[A]`.
   *
   * This is modeled as a separate, named, subtype because Option decoders
   * often have special semantics around the handling of `JNull`. By having
   * this as a named subtype, we premit certain optimizations that would
   * otherwise not be possible. See `circe-generic-extras` for some examples.
   */
  final class OptionDecoder[A](implicit A: Decoder[A]) extends Decoder[Option[A]] {
    final override def apply(c: HCursor): Result[Option[A]] = tryDecode(c)

    final override def tryDecode(c: ACursor): Decoder.Result[Option[A]] = c match {
      case c: HCursor =>
        if (c.value.isNull) rightNone
        else
          A(c) match {
            case Right(a) => Right(Some(a))
            case Left(df) => Left(df)
          }
      case c: FailedCursor =>
        if (!c.incorrectFocus) keyMissingNone
        else Left(DecodingFailure(MissingField, c.history))
    }

    final override def decodeAccumulating(c: HCursor): AccumulatingResult[Option[A]] = tryDecodeAccumulating(c)

    final override def tryDecodeAccumulating(c: ACursor): AccumulatingResult[Option[A]] = c match {
      case c: HCursor =>
        if (c.value.isNull) validNone
        else
          A.decodeAccumulating(c) match {
            case Valid(a)       => Valid(Some(a))
            case i @ Invalid(_) => i
          }
      case c: FailedCursor =>
        if (!c.incorrectFocus) keyMissingNoneAccumulating
        else Validated.invalidNel(DecodingFailure(MissingField, c.history))
    }
  }

  /**
   * A decoder for `NullOr[A]`.
   *
   * This is modeled as a separate, named, subtype because NullOr decoders
   * have special semantics around the handling of `JNull`.
   */
  final class NullOrDecoder[A](implicit A: Decoder[A]) extends Decoder[NullOr[A]] {
    override def apply(c: HCursor): Result[NullOr[A]] = tryDecode(c)

    override def tryDecode(c: ACursor): Decoder.Result[NullOr[A]] = {
      c match {
        case c: HCursor =>
          if (c.value.isNull) rightNull
          else
            A(c) match {
              case Right(a) => Right(NullOr.Value(a))
              case Left(df) => Left(df)
            }
        case c: FailedCursor =>
          Left(DecodingFailure(MissingField, c.history))
      }
    }

    override def decodeAccumulating(c: HCursor): AccumulatingResult[NullOr[A]] = tryDecodeAccumulating(c)

    override def tryDecodeAccumulating(c: ACursor): AccumulatingResult[NullOr[A]] = c match {
      case c: HCursor =>
        if (c.value.isNull) validNull
        else
          A.decodeAccumulating(c) match {
            case Valid(a)       => Valid(NullOr.Value(a))
            case i @ Invalid(_) => i
          }
      case c: FailedCursor =>
        Validated.invalidNel(DecodingFailure(MissingField, c.history))
    }
  }

  /**
   * A decoder for `Option[NullOr[A]]`.
   *
   * This is modeled as a separate, named, subtype because Option[NullOr] decoders
   * have semantics different from Option decoders.
   */
  final class OptionOfNullOrDecoder[A](implicit A: Decoder[A]) extends Decoder[Option[NullOr[A]]] {
    final override def apply(c: HCursor): Result[Option[NullOr[A]]] = tryDecode(c)

    final override def tryDecode(c: ACursor): Decoder.Result[Option[NullOr[A]]] = c match {
      case c: HCursor =>
        if (c.value.isNull) rightSomeNull
        else
          A(c) match {
            case Right(a) => Right(Some(NullOr.Value(a)))
            case Left(df) => Left(df)
          }
      case c: FailedCursor =>
        if (!c.incorrectFocus) keyMissingNone
        else Left(DecodingFailure(MissingField, c.history))
    }

    final override def decodeAccumulating(c: HCursor): AccumulatingResult[Option[NullOr[A]]] = tryDecodeAccumulating(c)

    final override def tryDecodeAccumulating(c: ACursor): AccumulatingResult[Option[NullOr[A]]] = c match {
      case c: HCursor =>
        if (c.value.isNull) validSomeNull
        else
          A.decodeAccumulating(c) match {
            case Valid(a)       => Valid(Some(NullOr.Value(a)))
            case i @ Invalid(_) => i
          }
      case c: FailedCursor =>
        if (!c.incorrectFocus) keyMissingNoneAccumulating
        else Validated.invalidNel(DecodingFailure(MissingField, c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit final def decodeOption[A](implicit d: Decoder[A]): Decoder[Option[A]] = new OptionDecoder[A]

  /**
   * @group Decoding
   */
  implicit final def decodeNullOr[A](implicit d: Decoder[A]): Decoder[NullOr[A]] = new Decoder.NullOrDecoder[A]

  /**
   * @group Decoding
   */
  implicit final def decodeOptionNullOr[A](implicit d: Decoder[A]): Decoder[Option[NullOr[A]]] =
    new Decoder.OptionOfNullOrDecoder[A]

  /**
   * @group Decoding
   */
  implicit final def decodeSome[A](implicit d: Decoder[A]): Decoder[Some[A]] = d.map(Some(_))

  /**
   * @group Decoding
   */
  implicit final val decodeNone: Decoder[None.type] = new Decoder[None.type] {
    final def apply(c: HCursor): Result[None.type] = if (c.value.isNull) Right(None)
    else {
      Left(DecodingFailure(WrongTypeExpectation("null", c.value), c.history))
    }
    final override def tryDecode(c: ACursor): Decoder.Result[None.type] = c match {
      case c: HCursor =>
        if (c.value.isNull) rightNone
        else Left(DecodingFailure(WrongTypeExpectation("null", c.value), c.history))
      case c: FailedCursor =>
        if (!c.incorrectFocus) keyMissingNone
        else Left(DecodingFailure(MissingField, c.history))
    }

    final override def tryDecodeAccumulating(c: ACursor): AccumulatingResult[None.type] = c match {
      case c: HCursor =>
        if (c.value.isNull) validNone
        else Validated.invalidNel(DecodingFailure(WrongTypeExpectation("null", c.value), c.history))
      case c: FailedCursor =>
        if (!c.incorrectFocus) keyMissingNoneAccumulating
        else Validated.invalidNel(DecodingFailure(MissingField, c.history))
    }
  }

  /**
   * @group Collection
   */
  implicit final def decodeMap[K, V](implicit
    decodeK: KeyDecoder[K],
    decodeV: Decoder[V]
  ): Decoder[ImmutableMap[K, V]] = new MapDecoder[K, V, ImmutableMap](decodeK, decodeV) {
    final protected def createBuilder(): Builder[(K, V), ImmutableMap[K, V]] = ImmutableMap.newBuilder[K, V]
  }

  /**
   * @group Collection
   */
  implicit final def decodeSeq[A](implicit decodeA: Decoder[A]): Decoder[Seq[A]] = new SeqDecoder[A, Seq](decodeA) {
    final protected def createBuilder(): Builder[A, Seq[A]] = Seq.newBuilder[A]
  }

  /**
   * @group Collection
   */
  implicit final def decodeSet[A](implicit decodeA: Decoder[A]): Decoder[Set[A]] = new SeqDecoder[A, Set](decodeA) {
    final protected def createBuilder(): Builder[A, Set[A]] = Set.newBuilder[A]
  }

  /**
   * @group Collection
   */
  implicit final def decodeList[A](implicit decodeA: Decoder[A]): Decoder[List[A]] = new SeqDecoder[A, List](decodeA) {
    final protected def createBuilder(): Builder[A, List[A]] = List.newBuilder[A]
  }

  /**
   * @group Collection
   */
  implicit final def decodeVector[A](implicit decodeA: Decoder[A]): Decoder[Vector[A]] =
    new SeqDecoder[A, Vector](decodeA) {
      final protected def createBuilder(): Builder[A, Vector[A]] = Vector.newBuilder[A]
    }

  private[this] class ChainBuilder[A] extends CompatBuilder[A, Chain[A]] {
    private[this] var xs: Chain[A] = Chain.nil
    final def clear(): Unit = xs = Chain.nil
    final def result(): Chain[A] = xs

    final def addOne(elem: A): this.type = {
      xs = xs.append(elem)
      this
    }
  }

  /**
   * @group Collection
   */
  implicit final def decodeChain[A](implicit decodeA: Decoder[A]): Decoder[Chain[A]] =
    new SeqDecoder[A, Chain](decodeA) {
      final protected def createBuilder(): Builder[A, Chain[A]] = new ChainBuilder[A]
    }

  /**
   * @group Collection
   */
  implicit final def decodeNonEmptyList[A](implicit decodeA: Decoder[A]): Decoder[NonEmptyList[A]] =
    new NonEmptySeqDecoder[A, List, NonEmptyList[A]](decodeA) {
      final protected def createBuilder(): Builder[A, List[A]] = List.newBuilder[A]
      final protected val create: (A, List[A]) => NonEmptyList[A] = (h, t) => NonEmptyList(h, t)
    }

  /**
   * @group Collection
   */
  implicit final def decodeNonEmptySeq[A](implicit decodeA: Decoder[A]): Decoder[NonEmptySeq[A]] =
    new NonEmptySeqDecoder[A, List, NonEmptySeq[A]](decodeA) {
      final protected def createBuilder(): Builder[A, List[A]] = List.newBuilder[A]
      final protected val create: (A, List[A]) => NonEmptySeq[A] = (h, t) => NonEmptySeq(h, t)
    }

  /**
   * @group Collection
   */
  implicit final def decodeNonEmptyVector[A](implicit decodeA: Decoder[A]): Decoder[NonEmptyVector[A]] =
    new NonEmptySeqDecoder[A, Vector, NonEmptyVector[A]](decodeA) {
      final protected def createBuilder(): Builder[A, Vector[A]] = Vector.newBuilder[A]
      final protected val create: (A, Vector[A]) => NonEmptyVector[A] = (h, t) => NonEmptyVector(h, t)
    }

  /**
   * @group Collection
   */
  implicit final def decodeNonEmptySet[A](implicit decodeA: Decoder[A], orderA: Order[A]): Decoder[NonEmptySet[A]] =
    new NonEmptySeqDecoder[A, SortedSet, NonEmptySet[A]](decodeA) {
      final protected def createBuilder(): Builder[A, SortedSet[A]] =
        SortedSet.newBuilder[A](Order.catsKernelOrderingForOrder(orderA))
      final protected val create: (A, SortedSet[A]) => NonEmptySet[A] =
        (h, t) => NonEmptySet(h, t)
    }

  /**
   * @group Collection
   */
  implicit final def decodeNonEmptyMap[K, V](implicit
    decodeK: KeyDecoder[K],
    orderK: Order[K],
    decodeV: Decoder[V]
  ): Decoder[NonEmptyMap[K, V]] =
    new MapDecoder[K, V, SortedMap](decodeK, decodeV) {
      final protected def createBuilder(): Builder[(K, V), SortedMap[K, V]] =
        SortedMap.newBuilder[K, V](Order.catsKernelOrderingForOrder(orderK))
    }.emap { map =>
      NonEmptyMap.fromMap(map).toRight("[K, V]NonEmptyMap[K, V]")
    }

  /**
   * @group Collection
   */
  implicit final def decodeNonEmptyChain[A](implicit decodeA: Decoder[A]): Decoder[NonEmptyChain[A]] =
    new NonEmptySeqDecoder[A, Chain, NonEmptyChain[A]](decodeA) {
      final protected def createBuilder(): Builder[A, Chain[A]] = new ChainBuilder[A]
      final protected val create: (A, Chain[A]) => NonEmptyChain[A] =
        (h, t) => NonEmptyChain.fromChainPrepend(h, t)
    }

  /**
   * @group Disjunction
   */
  final def decodeEither[A, B](leftKey: String, rightKey: String)(implicit
    decodeA: Decoder[A],
    decodeB: Decoder[B]
  ): Decoder[Either[A, B]] = new Decoder[Either[A, B]] {
    private[this] def failure(c: HCursor): Decoder.Result[Either[A, B]] =
      Left(DecodingFailure(MissingField, c.history))

    final def apply(c: HCursor): Result[Either[A, B]] = {
      val lf = c.downField(leftKey)
      val rf = c.downField(rightKey)

      lf match {
        case lc: HCursor =>
          rf match {
            case _: HCursor => failure(c)
            case rc =>
              decodeA(lc) match {
                case Right(v)    => Right(Left(v))
                case l @ Left(_) => l.asInstanceOf[Result[Either[A, B]]]
              }
          }
        case _ =>
          rf match {
            case rc: HCursor =>
              decodeB(rc) match {
                case Right(v)    => Right(Right(v))
                case l @ Left(_) => l.asInstanceOf[Result[Either[A, B]]]
              }
            case rc => failure(c)
          }
      }
    }
  }

  /**
   * @group Disjunction
   */
  final def decodeValidated[E, A](failureKey: String, successKey: String)(implicit
    decodeE: Decoder[E],
    decodeA: Decoder[A]
  ): Decoder[Validated[E, A]] =
    decodeEither[E, A](
      failureKey,
      successKey
    ).map(Validated.fromEither).withErrorMessage("[E, A]Validated[E, A]")

  private[this] abstract class JavaTimeDecoder[A](name: String) extends Decoder[A] {
    protected[this] def parseUnsafe(input: String): A

    /**
     * Add information from the `DateTimeException` to the `DecodingFailure` error message.
     */
    protected[this] def formatMessage(input: String, message: String): String

    final def apply(c: HCursor): Decoder.Result[A] = c.value match {
      case Json.JString(string) =>
        try Right(parseUnsafe(string))
        catch {
          case e: DateTimeException =>
            val message = e.getMessage

            if (message.eq(null)) Left(DecodingFailure("Couldn't decode time", c.history))
            else {
              val newMessage = formatMessage(string, message)
              Left(DecodingFailure(message = newMessage, ops = c.history))
            }
        }
      case json => Left(DecodingFailure(WrongTypeExpectation("string", json), c.history))
    }
  }

  private[this] abstract class StandardJavaTimeDecoder[A](name: String) extends JavaTimeDecoder[A](name) {
    protected[this] final def formatMessage(input: String, message: String): String = message
  }

  /**
   * @group Time
   */
  implicit final lazy val decodeDuration: Decoder[Duration] =
    new JavaTimeDecoder[Duration]("Duration") {
      protected[this] final def parseUnsafe(input: String): Duration = Duration.parse(input)

      // For some reason the error message for `Duration` does not contain the
      // input string by default.
      protected[this] final def formatMessage(input: String, message: String): String =
        s"Text '$input' cannot be parsed to a Duration"
    }

  /**
   * @group Time
   */
  implicit final lazy val decodeInstant: Decoder[Instant] =
    new StandardJavaTimeDecoder[Instant]("Instant") {
      protected[this] final def parseUnsafe(input: String): Instant = Instant.parse(input)
    }

  /**
   * @group Time
   */
  implicit final lazy val decodePeriod: Decoder[Period] =
    new JavaTimeDecoder[Period]("Period") {
      protected[this] final def parseUnsafe(input: String): Period = Period.parse(input)

      // For some reason the error message for `Period` does not contain the
      // input string by default.
      protected[this] final def formatMessage(input: String, message: String): String =
        s"Text '$input' cannot be parsed to a Period"
    }

  /**
   * @group Time
   */
  implicit final lazy val decodeZoneId: Decoder[ZoneId] =
    new StandardJavaTimeDecoder[ZoneId]("ZoneId") {
      protected[this] final def parseUnsafe(input: String): ZoneId = ZoneId.of(input)
    }

  /**
   * @group Time
   */
  final def decodeLocalDateWithFormatter(formatter: DateTimeFormatter): Decoder[LocalDate] =
    new StandardJavaTimeDecoder[LocalDate]("LocalDate") {
      protected[this] final def parseUnsafe(input: String): LocalDate =
        LocalDate.parse(input, formatter)
    }

  /**
   * @group Time
   */
  final def decodeLocalTimeWithFormatter(formatter: DateTimeFormatter): Decoder[LocalTime] =
    new StandardJavaTimeDecoder[LocalTime]("LocalTime") {
      protected[this] final def parseUnsafe(input: String): LocalTime =
        LocalTime.parse(input, formatter)
    }

  /**
   * @group Time
   */
  final def decodeLocalDateTimeWithFormatter(formatter: DateTimeFormatter): Decoder[LocalDateTime] =
    new StandardJavaTimeDecoder[LocalDateTime]("LocalDateTime") {
      protected[this] final def parseUnsafe(input: String): LocalDateTime =
        LocalDateTime.parse(input, formatter)
    }

  /**
   * @group Time
   */
  final def decodeMonthDayWithFormatter(formatter: DateTimeFormatter): Decoder[MonthDay] =
    new StandardJavaTimeDecoder[MonthDay]("MonthDay") {
      protected[this] final def parseUnsafe(input: String): MonthDay =
        MonthDay.parse(input, formatter)
    }

  /**
   * @group Time
   */
  final def decodeOffsetTimeWithFormatter(formatter: DateTimeFormatter): Decoder[OffsetTime] =
    new StandardJavaTimeDecoder[OffsetTime]("OffsetTime") {
      protected[this] final def parseUnsafe(input: String): OffsetTime =
        OffsetTime.parse(input, formatter)
    }

  /**
   * @group Time
   */
  final def decodeOffsetDateTimeWithFormatter(formatter: DateTimeFormatter): Decoder[OffsetDateTime] =
    new StandardJavaTimeDecoder[OffsetDateTime]("OffsetDateTime") {
      protected[this] final def parseUnsafe(input: String): OffsetDateTime =
        OffsetDateTime.parse(input, formatter)
    }

  /**
   * @group Time
   */
  final def decodeYearWithFormatter(formatter: DateTimeFormatter): Decoder[Year] =
    new StandardJavaTimeDecoder[Year]("Year") {
      protected[this] final def parseUnsafe(input: String): Year =
        Year.parse(input, formatter)
    }

  /**
   * @group Time
   */
  final def decodeYearMonthWithFormatter(formatter: DateTimeFormatter): Decoder[YearMonth] =
    new StandardJavaTimeDecoder[YearMonth]("YearMonth") {
      protected[this] final def parseUnsafe(input: String): YearMonth =
        YearMonth.parse(input, formatter)
    }

  /**
   * @group Time
   */
  final def decodeZonedDateTimeWithFormatter(formatter: DateTimeFormatter): Decoder[ZonedDateTime] =
    new StandardJavaTimeDecoder[ZonedDateTime]("ZonedDateTime") {
      protected[this] final def parseUnsafe(input: String): ZonedDateTime =
        ZonedDateTime.parse(input, formatter)
    }

  /**
   * @group Time
   */
  final def decodeZoneOffsetWithFormatter(formatter: DateTimeFormatter): Decoder[ZoneOffset] =
    new StandardJavaTimeDecoder[ZoneOffset]("ZoneOffset") {
      protected[this] final def parseUnsafe(input: String): ZoneOffset = ZoneOffset.of(input)
    }

  /**
   * @group Time
   */
  implicit final lazy val decodeLocalDate: Decoder[LocalDate] =
    new StandardJavaTimeDecoder[LocalDate]("LocalDate") {
      protected[this] final def parseUnsafe(input: String): LocalDate = LocalDate.parse(input)
    }

  /**
   * @group Time
   */
  implicit final lazy val decodeLocalTime: Decoder[LocalTime] =
    new StandardJavaTimeDecoder[LocalTime]("LocalTime") {
      protected[this] final def parseUnsafe(input: String): LocalTime = LocalTime.parse(input)
    }

  /**
   * @group Time
   */
  implicit final lazy val decodeLocalDateTime: Decoder[LocalDateTime] =
    new StandardJavaTimeDecoder[LocalDateTime]("LocalDateTime") {
      protected[this] final def parseUnsafe(input: String): LocalDateTime = LocalDateTime.parse(input)
    }

  /**
   * @group Time
   */
  implicit final lazy val decodeMonthDay: Decoder[MonthDay] =
    new StandardJavaTimeDecoder[MonthDay]("MonthDay") {
      protected[this] final def parseUnsafe(input: String): MonthDay = MonthDay.parse(input)
    }

  /**
   * @group Time
   */
  implicit final lazy val decodeOffsetTime: Decoder[OffsetTime] =
    new StandardJavaTimeDecoder[OffsetTime]("OffsetTime") {
      protected[this] final def parseUnsafe(input: String): OffsetTime = OffsetTime.parse(input)
    }

  /**
   * @group Time
   */
  implicit final lazy val decodeOffsetDateTime: Decoder[OffsetDateTime] =
    new StandardJavaTimeDecoder[OffsetDateTime]("OffsetDateTime") {
      protected[this] final def parseUnsafe(input: String): OffsetDateTime = OffsetDateTime.parse(input)
    }

  /**
   * @group Time
   */
  implicit final lazy val decodeYear: Decoder[Year] =
    new StandardJavaTimeDecoder[Year]("Year") {
      protected[this] final def parseUnsafe(input: String): Year = Year.parse(input)
    }

  /**
   * @group Time
   */
  implicit final lazy val decodeYearMonth: Decoder[YearMonth] =
    new StandardJavaTimeDecoder[YearMonth]("YearMonth") {
      protected[this] final def parseUnsafe(input: String): YearMonth = YearMonth.parse(input)
    }

  /**
   * @group Time
   */
  implicit final lazy val decodeZonedDateTime: Decoder[ZonedDateTime] =
    new StandardJavaTimeDecoder[ZonedDateTime]("ZonedDateTime") {
      protected[this] final def parseUnsafe(input: String): ZonedDateTime = ZonedDateTime.parse(input)
    }

  /**
   * @group Time
   */
  implicit final lazy val decodeZoneOffset: Decoder[ZoneOffset] =
    new StandardJavaTimeDecoder[ZoneOffset]("ZoneOffset") {
      protected[this] final def parseUnsafe(input: String): ZoneOffset = ZoneOffset.of(input)
    }

  private final case class DeferredDecoder[A](decoder: () => Decoder[A]) extends Decoder[A] {
    private lazy val resolved: Decoder[A] = resolve(decoder)

    @annotation.tailrec
    private def resolve(f: () => Decoder[A]): Decoder[A] =
      f() match {
        case DeferredDecoder(f) => resolve(f)
        case next               => next
      }

    override def apply(c: HCursor): Result[A] = resolved(c)

    override def decodeAccumulating(c: HCursor): AccumulatingResult[A] = resolved.decodeAccumulating(c)
  }
  implicit val decoderDefer: Defer[Decoder] = new Defer[Decoder] {
    override def defer[A](fa: => Decoder[A]): Decoder[A] = DeferredDecoder(() => fa)
  }

  /**
   * @group Instances
   */
  implicit final val decoderInstances: SemigroupK[Decoder] with MonadError[Decoder, DecodingFailure] =
    new SemigroupK[Decoder] with MonadError[Decoder, DecodingFailure] {
      final def combineK[A](x: Decoder[A], y: Decoder[A]): Decoder[A] = x.or(y)
      final def pure[A](a: A): Decoder[A] = const(a)
      override final def map[A, B](fa: Decoder[A])(f: A => B): Decoder[B] = fa.map(f)
      override final def product[A, B](fa: Decoder[A], fb: Decoder[B]): Decoder[(A, B)] = fa.product(fb)
      override final def ap[A, B](ff: Decoder[A => B])(fa: Decoder[A]): Decoder[B] = ff.product(fa).map {
        case (f, a) => f(a)
      }
      override final def ap2[A, B, Z](ff: Decoder[(A, B) => Z])(fa: Decoder[A], fb: Decoder[B]): Decoder[Z] =
        ff.product(fa.product(fb)).map {
          case (f, (a, b)) => f(a, b)
        }
      override final def map2[A, B, Z](fa: Decoder[A], fb: Decoder[B])(f: (A, B) => Z): Decoder[Z] =
        fa.product(fb).map {
          case (a, b) => f(a, b)
        }
      override final def map2Eval[A, B, Z](fa: Decoder[A], fb: Eval[Decoder[B]])(f: (A, B) => Z): Eval[Decoder[Z]] =
        fb.map(fb => map2(fa, fb)(f))
      override final def productR[A, B](fa: Decoder[A])(fb: Decoder[B]): Decoder[B] = fa.product(fb).map(_._2)
      override final def productL[A, B](fa: Decoder[A])(fb: Decoder[B]): Decoder[A] = fa.product(fb).map(_._1)

      final def flatMap[A, B](fa: Decoder[A])(f: A => Decoder[B]): Decoder[B] = fa.flatMap(f)

      final def raiseError[A](e: DecodingFailure): Decoder[A] = Decoder.failed(e)
      final def handleErrorWith[A](fa: Decoder[A])(f: DecodingFailure => Decoder[A]): Decoder[A] = fa.handleErrorWith(f)

      final def tailRecM[A, B](a: A)(f: A => Decoder[Either[A, B]]): Decoder[B] = new Decoder[B] {
        @tailrec
        private[this] def step(c: HCursor, a1: A): Result[B] = f(a1)(c) match {
          case l @ Left(_)     => l.asInstanceOf[Result[B]]
          case Right(Left(a2)) => step(c, a2)
          case Right(Right(b)) => Right(b)
        }

        final def apply(c: HCursor): Result[B] = step(c, a)
      }
    }

  implicit final lazy val currencyDecoder: Decoder[Currency] =
    Decoder[String].emap(value =>
      catsStdInstancesForEither[Throwable]
        .catchNonFatal(
          Currency.getInstance(value)
        )
        .leftMap((t: Throwable) =>
          // As of JRE 15 `.getMessage` and `.getLocalizedMessage` return
          // `null`, but that doesn't mean they always will.
          Option(t.getLocalizedMessage()).getOrElse(s"Unknown or unimplemented currency value: $value")
        )
    )

  /**
   * Helper methods for working with [[cats.data.StateT]] values that transform
   * the [[ACursor]].
   *
   * @group Utilities
   */
  object state {

    /**
     * Attempt to decode a value at key `k` and remove it from the [[ACursor]].
     */
    def decodeField[A: Decoder](k: String): StateT[Result, ACursor, A] = StateT[Result, ACursor, A] { c =>
      val field = c.downField(k)

      field.as[A] match {
        case Right(a) if field.failed => Right((c, a))
        case Right(a)                 => Right((field.delete, a))
        case l @ Left(_)              => l.asInstanceOf[Result[(ACursor, A)]]
      }
    }

    /**
     * Require the [[ACursor]] to be empty, using the provided function to
     * create the failure error message if it's not.
     */
    def requireEmptyWithMessage(createMessage: List[String] => String): StateT[Result, ACursor, Unit] =
      StateT[Result, ACursor, Unit] { c =>
        val keys = c.focus.flatMap(_.asObject).toList.flatMap(_.keys)

        if (keys.isEmpty) Right((c, ())) else Left(DecodingFailure(createMessage(keys), c.history))
      }

    /**
     * Require the [[ACursor]] to be empty, with a default message.
     */
    val requireEmpty: StateT[Result, ACursor, Unit] = requireEmptyWithMessage { keys =>
      s"Leftover keys: ${keys.mkString(", ")}"
    }
  }
}

private[circe] trait LowPriorityDecoders {

  /**
   * @group Prioritization
   */
  implicit def importedDecoder[A](implicit exported: Exported[Decoder[A]]): Decoder[A] = exported.instance
}
