package io.circe

import cats.{ MonadError, SemigroupK }
import cats.data.{ Kleisli, NonEmptyList, NonEmptyVector, OneAnd, StateT, Validated }
import cats.data.Validated.{ Invalid, Valid }
import cats.instances.either.{ catsStdInstancesForEither, catsStdSemigroupKForEither }
import io.circe.export.Exported
import java.io.Serializable
import java.util.UUID
import scala.annotation.tailrec
import scala.collection.Map
import scala.collection.generic.CanBuildFrom
import scala.collection.immutable.{ Map => ImmutableMap, Set }
import scala.collection.mutable.Builder
import scala.util.{ Failure, Success, Try }

trait Decoder[A] extends Serializable { self =>
  /**
   * Decode the given [[HCursor]].
   */
  def apply(c: HCursor): Decoder.Result[A]

  private[circe] def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] = apply(c) match {
    case Right(a) => Validated.valid(a)
    case Left(e) => Validated.invalidNel(e)
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
    case _ => Left(
      DecodingFailure("Attempt to decode value on failed cursor", c.history)
    )
  }

  def tryDecodeAccumulating(c: ACursor): AccumulatingDecoder.Result[A] = c match {
    case hc: HCursor => decodeAccumulating(hc)
    case _ => Validated.invalidNel(
      DecodingFailure("Attempt to decode value on failed cursor", c.history)
    )
  }

  /**
   * Decode the given [[Json]] value.
   */
  final def decodeJson(j: Json): Decoder.Result[A] = apply(HCursor.fromJson(j))

  final def accumulating: AccumulatingDecoder[A] = AccumulatingDecoder.fromDecoder(self)

  /**
   * Map a function over this [[Decoder]].
   */
  final def map[B](f: A => B): Decoder[B] = new Decoder[B] {
    final def apply(c: HCursor): Decoder.Result[B] = tryDecode(c)
    override def tryDecode(c: ACursor): Decoder.Result[B] = self.tryDecode(c) match {
      case Right(a) => Right(f(a))
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[B]]
    }
    override final def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[B] =
      tryDecodeAccumulating(c)

    override final def tryDecodeAccumulating(c: ACursor): AccumulatingDecoder.Result[B] =
      self.tryDecodeAccumulating(c).map(f)
  }

  /**
   * Monadically bind a function over this [[Decoder]].
   */
  final def flatMap[B](f: A => Decoder[B]): Decoder[B] = new Decoder[B] {
    final def apply(c: HCursor): Decoder.Result[B] = self(c) match {
      case Right(a) => f(a)(c)
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[B]]
    }

    override def tryDecode(c: ACursor): Decoder.Result[B] = self.tryDecode(c) match {
      case Right(a) => f(a).tryDecode(c)
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[B]]
    }

    override final def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[B] =
      self.decodeAccumulating(c).andThen(result => f(result).decodeAccumulating(c))

    override final def tryDecodeAccumulating(c: ACursor): AccumulatingDecoder.Result[B] =
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
    final def apply(c: HCursor): Decoder.Result[A] =
      Decoder.resultInstance.handleErrorWith(self(c))(failure => f(failure)(c))

    override final def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] =
      AccumulatingDecoder.resultInstance.handleErrorWith(self.decodeAccumulating(c))(failures =>
        f(failures.head).decodeAccumulating(c)
      )
  }

  /**
   * Build a new instance with the specified error message.
   */
  final def withErrorMessage(message: String): Decoder[A] = new Decoder[A] {
    final def apply(c: HCursor): Decoder.Result[A] = self(c) match {
      case r @ Right(_) => r
      case Left(e) => Left(e.withMessage(message))
    }

    override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] =
      self.decodeAccumulating(c).leftMap(_.map(_.withMessage(message)))
  }

  /**
   * Build a new instance that fails if the condition does not hold.
   */
  final def validate(pred: HCursor => Boolean, message: => String): Decoder[A] = new Decoder[A] {
    final def apply(c: HCursor): Decoder.Result[A] =
      if (pred(c)) self(c) else Left(DecodingFailure(message, c.history))

    override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] =
      if (pred(c)) self.decodeAccumulating(c) else Validated.invalidNel(DecodingFailure(message, c.history))
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
    override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[(A, B)] =
      AccumulatingDecoder.resultInstance.product(self.decodeAccumulating(c), fb.decodeAccumulating(c))
  }

  /**
   * Choose the first succeeding decoder.
   */
  final def or[AA >: A](d: => Decoder[AA]): Decoder[AA] = new Decoder[AA] {
    final def apply(c: HCursor): Decoder.Result[AA] = self(c) match {
      case r @ Right(_) => r
      case Left(_) => d(c)
    }
  }

  /**
   * Choose the first succeeding decoder, wrapping the result in a disjunction.
   */
  final def either[B](decodeB: Decoder[B]): Decoder[Either[A, B]] = new Decoder[Either[A, B]] {
    final def apply(c: HCursor): Decoder.Result[Either[A, B]] = self(c) match {
      case Right(v) => Right(Left(v))
      case Left(_) => decodeB(c) match {
        case Right(v) => Right(Right(v))
        case l @ Left(_) => l.asInstanceOf[Decoder.Result[Either[A, B]]]
      }
    }
  }

  /**
   * Create a new decoder that performs some operation on the incoming JSON before decoding.
   */
  final def prepare(f: ACursor => ACursor): Decoder[A] = new Decoder[A] {
    final def apply(c: HCursor): Decoder.Result[A] = tryDecode(c)
    override def tryDecode(c: ACursor): Decoder.Result[A] = self.tryDecode(f(c))
    override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] =
      tryDecodeAccumulating(c)
    override def tryDecodeAccumulating(c: ACursor): AccumulatingDecoder.Result[A] =
      self.tryDecodeAccumulating(f(c))
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
        case Right(a) => f(a) match {
          case r @ Right(_) => r.asInstanceOf[Decoder.Result[B]]
          case Left(message) => Left(DecodingFailure(message, c.history))
        }
        case l @ Left(_) => l.asInstanceOf[Decoder.Result[B]]
      }

    override final def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[B] =
      tryDecodeAccumulating(c)

    override final def tryDecodeAccumulating(c: ACursor): AccumulatingDecoder.Result[B] =
      self.tryDecodeAccumulating(c) match {
        case Valid(a) => f(a) match {
          case Right(b) => Validated.valid(b)
          case Left(message) => Validated.invalidNel(DecodingFailure(message, c.history))
        }
        case l @ Invalid(_) => l.asInstanceOf[AccumulatingDecoder.Result[B]]
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
        case Right(a) => f(a) match {
          case Success(b) => Right(b)
          case Failure(t) => Left(DecodingFailure.fromThrowable(t, c.history))
        }
        case l @ Left(_) => l.asInstanceOf[Decoder.Result[B]]
      }

    override final def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[B] =
      tryDecodeAccumulating(c)

    override final def tryDecodeAccumulating(c: ACursor): AccumulatingDecoder.Result[B] =
      self.tryDecodeAccumulating(c) match {
        case Valid(a) => f(a) match {
          case Success(b) => Validated.valid(b)
          case Failure(t) => Validated.invalidNel(DecodingFailure.fromThrowable(t, c.history))
        }
        case l @ Invalid(_) => l.asInstanceOf[AccumulatingDecoder.Result[B]]
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
 * @groupprio Collection 4
 *
 * @groupname Disjunction Disjunction instances
 * @groupdesc Disjunction Instance creation methods for disjunction-like types. Note that these
 * instances are not implicit, since they require non-obvious decisions about the names of the
 * discriminators. If you want instances for these types you can include the following import in
 * your program:
 * {{{
 *   import io.circe.disjunctionCodecs._
 * }}}
 * @groupprio Disjunction 5
 *
 * @groupname Instances Type class instances
 * @groupprio Instances 6
 *
 * @groupname Tuple Tuple instances
 * @groupprio Tuple 7
 *
 * @groupname Product Case class and other product instances
 * @groupprio Product 8
 *
 * @groupname Prioritization Instance prioritization
 * @groupprio Prioritization 9
 *
 * @author Travis Brown
 */
final object Decoder extends TupleDecoders with ProductDecoders with LowPriorityDecoders {
  /**
   * @group Aliases
   */
  type Result[A] = Either[DecodingFailure, A]

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
    final override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] =
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

    override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] = tryDecodeAccumulating(c)

    override def tryDecodeAccumulating(c: ACursor): AccumulatingDecoder.Result[A] = f(c) match {
      case Right(v) => Validated.valid(v)
      case Left(e) => Validated.invalidNel(e)
    }
  }

  /**
   * Construct an instance that always fails with the given [[DecodingFailure]].
   *
   * @group Utilities
   */
  final def failed[A](failure: DecodingFailure): Decoder[A] = new Decoder[A] {
    final def apply(c: HCursor): Result[A] = Left(failure)
    override final def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] =
      Validated.invalidNel(failure)
  }

  /**
   * Construct an instance that always fails with the given error message.
   *
   * @group Utilities
   */
  final def failedWithMessage[A](message: String): Decoder[A] = new Decoder[A] {
    final def apply(c: HCursor): Result[A] = Left(DecodingFailure(message, c.history))
    override final def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] =
      Validated.invalidNel(DecodingFailure(message, c.history))
  }

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
      case None => Left(DecodingFailure("JsonObject", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit final val decodeJsonNumber: Decoder[JsonNumber] = new Decoder[JsonNumber] {
    final def apply(c: HCursor): Result[JsonNumber] = c.value.asNumber match {
      case Some(v) => Right(v)
      case None => Left(DecodingFailure("JsonNumber", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit final val decodeString: Decoder[String] = new Decoder[String] {
    final def apply(c: HCursor): Result[String] = c.value match {
      case Json.JString(string) => Right(string)
      case _ => Left(DecodingFailure("String", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit final val decodeUnit: Decoder[Unit] = new Decoder[Unit] {
    final def apply(c: HCursor): Result[Unit] = c.value match {
      case Json.JObject(obj) if obj.isEmpty => Right(())
      case Json.JArray(arr) if arr.isEmpty => Right(())
      case other if other.isNull => Right(())
      case _ => Left(DecodingFailure("Unit", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit final val decodeBoolean: Decoder[Boolean] = new Decoder[Boolean] {
    final def apply(c: HCursor): Result[Boolean] = c.value match {
      case Json.JBoolean(b) => Right(b)
      case _ => Left(DecodingFailure("Boolean", c.history))
    }
  }

  /**
    * Decode a JSON value into a [[java.lang.Boolean]].
    *
    * @group Decoding
    */
  implicit final val decodeJavaBoolean: Decoder[java.lang.Boolean] = decodeBoolean.map(java.lang.Boolean.valueOf)

  /**
   * @group Decoding
   */
  implicit final val decodeChar: Decoder[Char] = new Decoder[Char] {
    final def apply(c: HCursor): Result[Char] = c.value match {
      case Json.JString(string) if string.length == 1 => Right(string.charAt(0))
      case _ => Left(DecodingFailure("Char", c.history))
    }
  }

  /**
    * Decode a JSON value into a [[java.lang.Character]].
    *
    * @group Decoding
    */
  implicit final val decodeJavaCharacter: Decoder[java.lang.Character] = decodeChar.map(java.lang.Character.valueOf)

  /**
   * Decode a JSON value into a [[scala.Float]].
   *
   * See [[decodeDouble]] for discussion of the approach taken for floating-point decoding.
   *
   * @group Decoding
   */
  implicit final val decodeFloat: Decoder[Float] = new DecoderWithFailure[Float]("Float") {
    final def apply(c: HCursor): Result[Float] = c.value match {
      case Json.JNumber(number) => Right(number.toDouble.toFloat)
      case Json.JString(string) => JsonNumber.fromString(string).map(_.toDouble.toFloat) match {
        case Some(v) => Right(v)
        case None => fail(c)
      }
      case other if other.isNull => Right(Float.NaN)
      case _ => fail(c)
    }
  }

  /**
    * Decode a JSON value into a [[java.lang.Float]].
    *
    * @group Decoding
    */
  implicit final val decodeJavaFloat: Decoder[java.lang.Float] = decodeFloat.map(java.lang.Float.valueOf)

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
      case Json.JString(string) => JsonNumber.fromString(string).map(_.toDouble) match {
        case Some(v) => Right(v)
        case None => fail(c)
      }
      case other if other.isNull => Right(Double.NaN)
      case _ => fail(c)
    }
  }

  /**
    * Decode a JSON value into a [[java.lang.Double]].
    *
    * @group Decoding
    */
  implicit final val decodeJavaDouble: Decoder[java.lang.Double] = decodeDouble.map(java.lang.Double.valueOf)

  /**
   * Decode a JSON value into a [[scala.Byte]].
   *
   * See [[decodeLong]] for discussion of the approach taken for integral decoding.
   *
   * @group Decoding
   */
  implicit final val decodeByte: Decoder[Byte] = new DecoderWithFailure[Byte]("Byte") {
    final def apply(c: HCursor): Result[Byte] = c.value match {
      case Json.JNumber(number) => number.toByte match {
        case Some(v) => Right(v)
        case None => fail(c)
      }
      case Json.JString(string) => JsonNumber.fromString(string).flatMap(_.toByte) match {
        case Some(value) => Right(value)
        case None => fail(c)
      }
      case _ => fail(c)
    }
  }

  /**
    * Decode a JSON value into a [[java.lang.Byte]].
    *
    * @group Decoding
    */
  implicit final val decodeJavaByte: Decoder[java.lang.Byte] = decodeByte.map(java.lang.Byte.valueOf)

  /**
   * Decode a JSON value into a [[scala.Short]].
   *
   * See [[decodeLong]] for discussion of the approach taken for integral decoding.
   *
   * @group Decoding
   */
  implicit final val decodeShort: Decoder[Short] = new DecoderWithFailure[Short]("Short") {
    final def apply(c: HCursor): Result[Short] = c.value match {
      case Json.JNumber(number) => number.toShort match {
        case Some(v) => Right(v)
        case None => fail(c)
      }
      case Json.JString(string) => JsonNumber.fromString(string).flatMap(_.toShort) match {
        case Some(value) => Right(value)
        case None => fail(c)
      }
      case _ => fail(c)
    }
  }

  /**
    * Decode a JSON value into a [[java.lang.Short]].
    *
    * @group Decoding
    */
  implicit final val decodeJavaShort: Decoder[java.lang.Short] = decodeShort.map(java.lang.Short.valueOf)

  /**
    * Decode a JSON value into a [[scala.Int]].
    *
    * See [[decodeLong]] for discussion of the approach taken for integral decoding.
    *
    * @group Decoding
    */
  implicit final val decodeInt: Decoder[Int] = new DecoderWithFailure[Int]("Int") {
    final def apply(c: HCursor): Result[Int] = c.value match {
      case Json.JNumber(number) => number.toInt match {
        case Some(v) => Right(v)
        case None => fail(c)
      }
      case Json.JString(string) => JsonNumber.fromString(string).flatMap(_.toInt) match {
        case Some(value) => Right(value)
        case None => fail(c)
      }
      case _ => fail(c)
    }
  }

  /**
    * Decode a JSON value into a [[java.lang.Integer]].
    *
    * @group Decoding
    */
  implicit final val decodeJavaInteger: Decoder[java.lang.Integer] = decodeInt.map(java.lang.Integer.valueOf)

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
      case Json.JNumber(number) => number.toLong match {
        case Some(v) => Right(v)
        case None => fail(c)
      }
      case Json.JString(string) => JsonNumber.fromString(string).flatMap(_.toLong) match {
        case Some(value) => Right(value)
        case None => fail(c)
      }
      case _ => fail(c)
    }
  }

  /**
    * Decode a JSON value into a [[java.lang.Long]].
    *
    * @group Decoding
    */
  implicit final val decodeJavaLong: Decoder[java.lang.Long] = decodeLong.map(java.lang.Long.valueOf)

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
      case Json.JNumber(number) => number.toBigInt match {
        case Some(v) => Right(v)
        case None => fail(c)
      }
      case Json.JString(string) => JsonNumber.fromString(string).flatMap(_.toBigInt) match {
        case Some(value) => Right(value)
        case None => fail(c)
      }
      case _ => fail(c)
    }
  }

  /**
    * Decode a JSON value into a [[java.math.BigInteger]].
    *
    * @group Decoding
    */
  implicit final val decodeJavaBigInteger: Decoder[java.math.BigInteger] = decodeBigInt.map(_.bigInteger)

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
      case Json.JNumber(number) => number.toBigDecimal match {
        case Some(v) => Right(v)
        case None => fail(c)
      }
      case Json.JString(string) => JsonNumber.fromString(string).flatMap(_.toBigDecimal) match {
        case Some(value) => Right(value)
        case None => fail(c)
      }
      case _ => fail(c)
    }
  }

  /**
    * Decode a JSON value into a [[java.math.BigDecimal]].
    *
    * @group Decoding
    */
  implicit final val decodeJavaBigDecimal: Decoder[java.math.BigDecimal] = decodeBigDecimal.map(_.bigDecimal)

  /**
   * @group Decoding
   */
  implicit final val decodeUUID: Decoder[UUID] = new Decoder[UUID] {
    private[this] def fail(c: HCursor): Result[UUID] = Left(DecodingFailure("UUID", c.history))

    final def apply(c: HCursor): Result[UUID] = c.value match {
      case Json.JString(string) if string.length == 36 => try Right(UUID.fromString(string)) catch {
        case _: IllegalArgumentException => fail(c)
      }
      case _ => fail(c)
    }
  }

  private[this] final val rightNone: Either[DecodingFailure, Option[Nothing]] = Right(None)

  /**
   * @group Decoding
   */
  implicit final def decodeOption[A](implicit d: Decoder[A]): Decoder[Option[A]] = withReattempt {
    case c: HCursor =>
      if (c.value.isNull) rightNone else d(c) match {
        case Right(a) => Right(Some(a))
        case Left(df) => Left(df)
      }
    case c: FailedCursor =>
      if (!c.incorrectFocus) rightNone else Left(DecodingFailure("[A]Option[A]", c.history))
  }

  /**
   * @group Decoding
   */
  implicit final def decodeSome[A](implicit d: Decoder[A]): Decoder[Some[A]] = d.map(Some(_))

  /**
   * @group Decoding
   */
  implicit final val decodeNone: Decoder[None.type] = new Decoder[None.type] {
    final def apply(c: HCursor): Result[None.type] = if (c.value.isNull) Right(None) else {
      Left(DecodingFailure("None", c.history))
    }
  }

  /**
   * @note The resulting instance will not be serializable (in the `java.io.Serializable` sense)
   *       unless the provided [[scala.collection.generic.CanBuildFrom]] is serializable.
   * @group Collection
   */
  implicit final def decodeMapLike[K, V, M[K, V] <: Map[K, V]](implicit
    decodeK: KeyDecoder[K],
    decodeV: Decoder[V],
    cbf: CanBuildFrom[Nothing, (K, V), M[K, V]]
  ): Decoder[M[K, V]] = new MapDecoder[K, V, M](decodeK, decodeV) {
    final protected def createBuilder(): Builder[(K, V), M[K, V]] = cbf()
  }

  /**
   * @note The resulting instance will not be serializable (in the `java.io.Serializable` sense)
   *       unless the provided [[scala.collection.generic.CanBuildFrom]] is serializable.
   * @group Collection
   */
  implicit final def decodeTraversable[A, C[A] <: Traversable[A]](implicit
    decodeA: Decoder[A],
    cbf: CanBuildFrom[Nothing, A, C[A]]
  ): Decoder[C[A]] = new SeqDecoder[A, C](decodeA) {
    final protected def createBuilder(): Builder[A, C[A]] = cbf.apply()
  }

  /**
   * @group Collection
   */
  implicit final def decodeArray[A](implicit
    decodeA: Decoder[A],
    cbf: CanBuildFrom[Nothing, A, Array[A]]
  ): Decoder[Array[A]] = new SeqDecoder[A, Array](decodeA) {
    final protected def createBuilder(): Builder[A, Array[A]] = cbf.apply()
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

  /**
   * @note The resulting instance will not be serializable (in the `java.io.Serializable` sense)
   *       unless the provided [[scala.collection.generic.CanBuildFrom]] is serializable.
   * @group Collection
   */
  implicit final def decodeOneAnd[A, C[_]](implicit
    decodeA: Decoder[A],
    cbf: CanBuildFrom[Nothing, A, C[A]]
  ): Decoder[OneAnd[C, A]] = new NonEmptySeqDecoder[A, C, OneAnd[C, A]](decodeA) {
    final protected def createBuilder(): Builder[A, C[A]] = cbf()
    final protected val create: (A, C[A]) => OneAnd[C, A] = (h, t) => OneAnd(h, t)
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
  implicit final def decodeNonEmptyVector[A](implicit decodeA: Decoder[A]): Decoder[NonEmptyVector[A]] =
    new NonEmptySeqDecoder[A, Vector, NonEmptyVector[A]](decodeA) {
      final protected def createBuilder(): Builder[A, Vector[A]] = Vector.newBuilder[A]
      final protected val create: (A, Vector[A]) => NonEmptyVector[A] = (h, t) => NonEmptyVector(h, t)
    }

  /**
   * @group Disjunction
   */
  final def decodeEither[A, B](leftKey: String, rightKey: String)(implicit
    da: Decoder[A],
    db: Decoder[B]
  ): Decoder[Either[A, B]] = new Decoder[Either[A, B]] {
    final def apply(c: HCursor): Result[Either[A, B]] = {
      val lf = c.downField(leftKey)
      val rf = c.downField(rightKey)

      lf match {
        case lc: HCursor =>
          rf match {
            case rc: HCursor => Left(DecodingFailure("[A, B]Either[A, B]", c.history))
            case rc => da(lc) match {
              case Right(v) => Right(Left(v))
              case l @ Left(_) => l.asInstanceOf[Result[Either[A, B]]]
            }
          }
        case lc =>
          rf match {
            case rc: HCursor => db(rc) match {
              case Right(v) => Right(Right(v))
              case l @ Left(_) => l.asInstanceOf[Result[Either[A, B]]]
            }
            case rc => Left(DecodingFailure("[A, B]Either[A, B]", c.history))
          }
      }
    }
  }

  /**
   * @group Disjunction
   */
  final def decodeValidated[E, A](failureKey: String, successKey: String)(implicit
    de: Decoder[E],
    da: Decoder[A]
  ): Decoder[Validated[E, A]] =
    decodeEither[E, A](
      failureKey,
      successKey
    ).map(Validated.fromEither).withErrorMessage("[E, A]Validated[E, A]")

  /**
   * @group Instances
   */
  final val resultInstance: MonadError[Result, DecodingFailure] = catsStdInstancesForEither[DecodingFailure]

  /**
   * @group Instances
   */
  implicit final val decoderInstances: SemigroupK[Decoder] with MonadError[Decoder, DecodingFailure] =
    new SemigroupK[Decoder] with MonadError[Decoder, DecodingFailure] {
      final def combineK[A](x: Decoder[A], y: Decoder[A]): Decoder[A] = x.or(y)
      final def pure[A](a: A): Decoder[A] = const(a)
      override final def map[A, B](fa: Decoder[A])(f: A => B): Decoder[B] = fa.map(f)
      override final def product[A, B](fa: Decoder[A], fb: Decoder[B]): Decoder[(A, B)] = fa.product(fb)
      final def flatMap[A, B](fa: Decoder[A])(f: A => Decoder[B]): Decoder[B] = fa.flatMap(f)

      final def raiseError[A](e: DecodingFailure): Decoder[A] = Decoder.failed(e)
      final def handleErrorWith[A](fa: Decoder[A])(f: DecodingFailure => Decoder[A]): Decoder[A] = fa.handleErrorWith(f)

      final def tailRecM[A, B](a: A)(f: A => Decoder[Either[A, B]]): Decoder[B] = new Decoder[B] {
        @tailrec
        private[this] def step(c: HCursor, a1: A): Result[B] = f(a1)(c) match {
          case l @ Left(_) => l.asInstanceOf[Result[B]]
          case Right(Left(a2)) => step(c, a2)
          case Right(Right(b)) => Right(b)
        }

        final def apply(c: HCursor): Result[B] = step(c, a)
      }
    }

  /**
   * {{{
   *   object WeekDay extends Enumeration { ... }
   *   implicit val weekDayDecoder = Decoder.enumDecoder(WeekDay)
   * }}}
   *
   * @group Utilities
   */
  final def enumDecoder[E <: Enumeration](enum: E): Decoder[E#Value] =
    Decoder.decodeString.flatMap { str =>
      Decoder.instanceTry { _ =>
        Try(enum.withName(str))
      }
    }

  /**
   * Helper methods for working with [[cats.data.StateT]] values that transform
   * the [[ACursor]].
   *
   * @group Utilities
   */
  final object state {
    /**
     * Attempt to decode a value at key `k` and remove it from the [[ACursor]].
     */
    def decodeField[A: Decoder](k: String): StateT[Result, ACursor, A] = StateT[Result, ACursor, A] { c =>
      val field = c.downField(k)

      field.as[A] match {
        case Right(a) if field.failed => Right((c, a))
        case Right(a) => Right((field.delete, a))
        case l @ Left(_) => l.asInstanceOf[Result[(ACursor, A)]]
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
      s"Leftover keys: ${ keys.mkString(", ") }"
    }
  }
}

private[circe] trait LowPriorityDecoders {
  /**
   * @group Prioritization
   */
  implicit def importedDecoder[A](implicit exported: Exported[Decoder[A]]): Decoder[A] = exported.instance
}
