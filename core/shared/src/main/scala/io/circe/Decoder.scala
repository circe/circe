package io.circe

import cats.{ MonadError, SemigroupK }
import cats.data.{ Kleisli, NonEmptyList, OneAnd, Validated, Xor }
import cats.std.list._
import io.circe.export.Exported
import java.util.UUID
import scala.collection.generic.CanBuildFrom
import scala.collection.mutable.Builder

trait Decoder[A] extends Serializable { self =>
  /**
   * Decode the given hcursor.
   */
  def apply(c: HCursor): Decoder.Result[A]

  private[circe] def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] =
    apply(c).toValidated.toValidatedNel

  /**
   * Decode the given acursor.
   */
  def tryDecode(c: ACursor): Decoder.Result[A] = if (c.succeeded) apply(c.any) else Xor.left(
    DecodingFailure("Attempt to decode value on failed cursor", c.any.history)
  )

  def tryDecodeAccumulating(c: ACursor): AccumulatingDecoder.Result[A] =
    if (c.succeeded) decodeAccumulating(c.any) else Validated.invalidNel(
      DecodingFailure("Attempt to decode value on failed cursor", c.history)
    )

  /**
   * Decode the given [[Json]] value.
   */
  final def decodeJson(j: Json): Decoder.Result[A] = apply(HCursor.fromCursor(j.cursor))

  final def accumulating: AccumulatingDecoder[A] = AccumulatingDecoder.fromDecoder(self)

  /**
   * Map a function over this [[Decoder]].
   */
  final def map[B](f: A => B): Decoder[B] = new Decoder[B] {
    final def apply(c: HCursor): Decoder.Result[B] = self(c).map(f)
    override def tryDecode(c: ACursor): Decoder.Result[B] = self.tryDecode(c).map(f)
    override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[B] =
      self.decodeAccumulating(c).map(f)
  }

  /**
   * Monadically bind a function over this [[Decoder]].
   */
  final def flatMap[B](f: A => Decoder[B]): Decoder[B] = new Decoder[B] {
    final def apply(c: HCursor): Decoder.Result[B] = self(c).flatMap(a => f(a)(c))

    override def tryDecode(c: ACursor): Decoder.Result[B] = {
      self.tryDecode(c).flatMap(a => f(a).tryDecode(c))
    }

    override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[B] =
      self.decodeAccumulating(c).andThen(result => f(result).decodeAccumulating(c))
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
    final def apply(c: HCursor): Decoder.Result[A] = self(c).leftMap(_.withMessage(message))

    override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] =
      self.decodeAccumulating(c).leftMap(_.map(_.withMessage(message)))
  }

  /**
   * Build a new instance that fails if the condition does not hold.
   */
  final def validate(pred: HCursor => Boolean, message: => String): Decoder[A] = new Decoder[A] {
    final def apply(c: HCursor): Decoder.Result[A] =
      if (pred(c)) apply(c) else Xor.left(DecodingFailure(message, c.history))
  }

  /**
   * Convert to a Kleisli arrow.
   */
  final def kleisli: Kleisli[Decoder.Result, HCursor, A] =
    Kleisli[Decoder.Result, HCursor, A](apply(_))

  /**
   * Run two decoders and return their results as a pair.
   */
  final def and[B](fb: Decoder[B]): Decoder[(A, B)] = new Decoder[(A, B)] {
    final def apply(c: HCursor): Decoder.Result[(A, B)] = Decoder.resultInstance.product(self(c), fb(c))
    override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[(A, B)] =
      AccumulatingDecoder.resultInstance.product(self.decodeAccumulating(c), fb.decodeAccumulating(c))
  }

  /**
   * Choose the first succeeding decoder.
   */
  final def or[AA >: A](d: => Decoder[AA]): Decoder[AA] = new Decoder[AA] {
    final def apply(c: HCursor): Decoder.Result[AA] = self(c) match {
      case r @ Xor.Right(_) => r
      case Xor.Left(_) => d(c)
    }
  }

  /**
   * Run one or another decoder.
   */
  final def split[B](d: Decoder[B]): Xor[HCursor, HCursor] => Decoder.Result[Xor[A, B]] = _ match {
    case Xor.Left(c) => self(c).map(Xor.left)
    case Xor.Right(c) => d(c).map(Xor.right)
  }

  /**
   * Run two decoders.
   */
  final def product[B](x: Decoder[B]): (HCursor, HCursor) => Decoder.Result[(A, B)] = (a1, a2) =>
    Decoder.resultInstance.product(self(a1), x(a2))

  /**
   * Create a new decoder that performs some operation on the incoming JSON before decoding.
   */
  final def prepare(f: HCursor => ACursor): Decoder[A] = new Decoder[A] {
    final def apply(c: HCursor): Decoder.Result[A] = self.tryDecode(f(c))
    override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] =
      self.tryDecodeAccumulating(f(c))
  }

  /**
   * Create a new decoder that performs some operation on the result if this one succeeds.
   *
   * @param f a function returning either a value or an error message
   */
  final def emap[B](f: A => Xor[String, B]): Decoder[B] = new Decoder[B] {
    final def apply(c: HCursor): Decoder.Result[B] =
      self(c).flatMap(a => f(a).leftMap(message => DecodingFailure(message, c.history)))
  }
}

/**
 * Utilities and instances for [[Decoder]].
 *
 * @groupname Utilities Miscellaneous utilities
 * @groupprio Utilities 0
 *
 * @groupname Decoding Decoder instances
 * @groupprio Decoding 2
 *
 * @groupname Disjunction Disjunction instances
 * @groupdesc Disjunction Instance creation methods for disjunction-like types. Note that these
 * instances are not implicit, since they require non-obvious decisions about the names of the
 * discriminators. If you want instances for these types you can include the following import in
 * your program:
 * {{{
 *   import io.circe.disjunctionCodecs._
 * }}}
 * @groupprio Disjunction 3
 *
 * @groupname Instances Type class instances
 * @groupprio Instances 4
 *
 * @groupname Tuple Tuple instances
 * @groupprio Tuple 5
 *
 * @groupname Product Case class and other product instances
 * @groupprio Product 6
 *
 * @author Travis Brown
 */
final object Decoder extends TupleDecoders with ProductDecoders with LowPriorityDecoders {
  import Json._

  type Result[A] = Xor[DecodingFailure, A]
  val resultInstance: MonadError[Result, DecodingFailure] = Xor.xorInstances[DecodingFailure]

  /**
   * Return an instance for a given type.
   *
   * @group Utilities
   */
  final def apply[A](implicit instance: Decoder[A]): Decoder[A] = instance

  /**
   * Construct an instance from a function.
   *
   * @group Utilities
   */
  final def instance[A](f: HCursor => Result[A]): Decoder[A] = new Decoder[A] {
    final def apply(c: HCursor): Result[A] = f(c)
  }

  /**
   * Construct an instance from a function that may reattempt on failure.
   *
   * @group Utilities
   */
  final def withReattempt[A](f: ACursor => Result[A]): Decoder[A] = new Decoder[A] {
    final def apply(c: HCursor): Result[A] = tryDecode(c.acursor)

    override def tryDecode(c: ACursor): Decoder.Result[A] = f(c)

    override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] =
      tryDecodeAccumulating(c.acursor)

    override def tryDecodeAccumulating(c: ACursor): AccumulatingDecoder.Result[A] =
      f(c).toValidated.toValidatedNel
  }

  /**
   * Construct an instance that always fails with the given [[DecodingFailure]].
   *
   * @group Utilities
   */
  final def failed[A](failure: DecodingFailure): Decoder[A] = new Decoder[A] {
    final def apply(c: HCursor): Result[A] = Xor.left(failure)
    override final def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] =
      Validated.invalidNel(failure)
  }

  /**
   * Construct an instance that always fails with the given error message.
   *
   * @group Utilities
   */
  final def failedWithMessage[A](message: String): Decoder[A] = failed(DecodingFailure(message, Nil))

  /**
   * @group Decoding
   */
  implicit final val decodeHCursor: Decoder[HCursor] = new Decoder[HCursor] {
    final def apply(c: HCursor): Result[HCursor] = Xor.right(c)
  }

  /**
   * @group Decoding
   */
  implicit final val decodeJson: Decoder[Json] = new Decoder[Json] {
    final def apply(c: HCursor): Result[Json] = Xor.right(c.focus)
  }

  /**
   * @group Decoding
   */
  implicit final val decodeJsonObject: Decoder[JsonObject] = new Decoder[JsonObject] {
    final def apply(c: HCursor): Result[JsonObject] =
      Xor.fromOption(c.focus.asObject, DecodingFailure("JsonObject", c.history))
  }

  /**
   * @group Decoding
   */
  implicit final val decodeJsonNumber: Decoder[JsonNumber] = new Decoder[JsonNumber] {
    final def apply(c: HCursor): Result[JsonNumber] =
      Xor.fromOption(c.focus.asNumber, DecodingFailure("JsonNumber", c.history))
  }

  /**
   * @group Decoding
   */
  implicit final val decodeString: Decoder[String] = new Decoder[String] {
    final def apply(c: HCursor): Result[String] = c.focus match {
      case JString(string) => Xor.right(string)
      case _ => Xor.left(DecodingFailure("String", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit final val decodeUnit: Decoder[Unit] = new Decoder[Unit] {
    final def apply(c: HCursor): Result[Unit] = c.focus match {
      case JNull => Xor.right(())
      case JObject(obj) if obj.isEmpty => Xor.right(())
      case JArray(arr) if arr.isEmpty => Xor.right(())
      case _ => Xor.left(DecodingFailure("Unit", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit final val decodeBoolean: Decoder[Boolean] = new Decoder[Boolean] {
    final def apply(c: HCursor): Result[Boolean] = c.focus match {
      case JBoolean(b) => Xor.right(b)
      case _ => Xor.left(DecodingFailure("Boolean", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit final val decodeChar: Decoder[Char] = new Decoder[Char] {
    final def apply(c: HCursor): Result[Char] = c.focus match {
      case JString(string) if string.length == 1 => Xor.right(string.charAt(0))
      case _ => Xor.left(DecodingFailure("Char", c.history))
    }
  }

  /**
   * Decode a JSON value into a [[scala.Float]].
   *
   * See [[decodeDouble]] for discussion of the approach taken for floating-point decoding.
   *
   * @group Decoding
   */
  implicit final val decodeFloat: Decoder[Float] = new Decoder[Float] {
    final def apply(c: HCursor): Result[Float] = c.focus match {
      case JNull => Xor.right(Float.NaN)
      case JNumber(number) => Xor.right(number.toDouble.toFloat)
      case JString(string) => Xor.fromOption(
        JsonNumber.fromString(string).map(_.toDouble.toFloat),
        DecodingFailure("Float", c.history)
      )
      case _ => Xor.left(DecodingFailure("Float", c.history))
    }
  }

  /**
   * Decode a JSON value into a [[scala.Double]].
   *
   * Unlike the integral decoders provided here, this decoder will accept values that are too large
   * to be represented and will return them as `PositiveInfinity` or `NegativeInfinity`, and it may
   * lose precision.
   *
   * @group Decoding
   */
  implicit final val decodeDouble: Decoder[Double] = new Decoder[Double] {
    final def apply(c: HCursor): Result[Double] = c.focus match {
      case JNull => Xor.right(Double.NaN)
      case JNumber(number) => Xor.right(number.toDouble)
      case JString(string) => Xor.fromOption(
        JsonNumber.fromString(string).map(_.toDouble),
        DecodingFailure("Double", c.history)
      )
      case _ => Xor.left(DecodingFailure("Double", c.history))
    }
  }

  /**
   * Decode a JSON value into a [[scala.Byte]].
   *
   * See [[decodeLong]] for discussion of the approach taken for integral decoding.
   *
   * @group Decoding
   */
  implicit final val decodeByte: Decoder[Byte] = new Decoder[Byte] {
    final def apply(c: HCursor): Result[Byte] = c.focus match {
      case JNumber(number) => Xor.fromOption(
        number.toByte,
        DecodingFailure("Byte", c.history)
      )
      case JString(string) => try {
        Xor.right(string.toByte)
      } catch {
        case _: NumberFormatException => Xor.left(DecodingFailure("Byte", c.history))
      }
      case _ => Xor.left(DecodingFailure("Byte", c.history))
    }
  }

  /**
   * Decode a JSON value into a [[scala.Short]].
   *
   * See [[decodeLong]] for discussion of the approach taken for integral decoding.
   *
   * @group Decoding
   */
  implicit final val decodeShort: Decoder[Short] = new Decoder[Short] {
    final def apply(c: HCursor): Result[Short] = c.focus match {
      case JNumber(number) => Xor.fromOption(
        number.toShort,
        DecodingFailure("Short", c.history)
      )
      case JString(string) => try {
        Xor.right(string.toShort)
      } catch {
        case _: NumberFormatException => Xor.left(DecodingFailure("Short", c.history))
      }
      case _ => Xor.left(DecodingFailure("Short", c.history))
    }
  }

  /**
   * Decode a JSON value into a [[scala.Int]].
   *
   * See [[decodeLong]] for discussion of the approach taken for integral decoding.
   *
   * @group Decoding
   */
  implicit final val decodeInt: Decoder[Int] = new Decoder[Int] {
    final def apply(c: HCursor): Result[Int] = c.focus match {
      case JNumber(number) => Xor.fromOption(
        number.toInt,
        DecodingFailure("Int", c.history)
      )
      case JString(string) => try {
        Xor.right(string.toInt)
      } catch {
        case _: NumberFormatException => Xor.left(DecodingFailure("Int", c.history))
      }
      case _ => Xor.left(DecodingFailure("Int", c.history))
    }
  }

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
  implicit final val decodeLong: Decoder[Long] = new Decoder[Long] {
    final def apply(c: HCursor): Result[Long] = c.focus match {
      case JNumber(number) => Xor.fromOption(
        number.toLong,
        DecodingFailure("Long", c.history)
      )
      case JString(string) => try {
        Xor.right(string.toLong)
      } catch {
        case _: NumberFormatException => Xor.left(DecodingFailure("Long", c.history))
      }
      case _ => Xor.left(DecodingFailure("Long", c.history))
    }
  }

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
  implicit final val decodeBigInt: Decoder[BigInt] = new Decoder[BigInt] {
    final def apply(c: HCursor): Result[BigInt] = c.focus match {
      case JNumber(number) => Xor.fromOption(
        number.toBigInt,
        DecodingFailure("BigInt", c.history)
      )
      case JString(string) => try {
        Xor.right(BigInt(string))
      } catch {
        case _: NumberFormatException => Xor.left(DecodingFailure("BigInt", c.history))
      }
      case _ => Xor.left(DecodingFailure("BigInt", c.history))
    }
  }

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
  implicit final val decodeBigDecimal: Decoder[BigDecimal] = new Decoder[BigDecimal] {
    final def apply(c: HCursor): Result[BigDecimal] = c.focus match {
      case JNumber(number) => Xor.fromOption(
        number.toBigDecimal,
        DecodingFailure("BigDecimal", c.history)
      )
      case JString(string) => try {
        Xor.right(BigDecimal(string))
      } catch {
        case _: NumberFormatException => Xor.left(DecodingFailure("BigDecimal", c.history))
      }
      case _ => Xor.left(DecodingFailure("BigDecimal", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit final val decodeUUID: Decoder[UUID] = new Decoder[UUID] {
    final def apply(c: HCursor): Result[UUID] = c.focus match {
      case JString(string) if string.length == 36 => try {
        Xor.right(UUID.fromString(string))
      } catch {
        case _: IllegalArgumentException => Xor.left(DecodingFailure("UUID", c.history))
      }
      case _ => Xor.left(DecodingFailure("UUID", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit final def decodeCanBuildFrom[A, C[_]](implicit
    d: Decoder[A],
    cbf: CanBuildFrom[Nothing, A, C[A]]
  ): Decoder[C[A]] = new Decoder[C[A]] {
    final def apply(c: HCursor): Decoder.Result[C[A]] = {
      val arrayCursor = c.downArray

      if (arrayCursor.succeeded) {
        arrayCursor.any.traverseDecode(cbf.apply)(
          _.right,
          (acc, hcursor) => hcursor.as[A] match {
            case Xor.Right(a) => Xor.Right(acc += a)
            case l @ Xor.Left(_) => l
          }
        ) match {
          case Xor.Right(builder) => Xor.right(builder.result)
          case l @ Xor.Left(_) => l
        }
      } else if (c.focus.isArray)
          Xor.right(cbf.apply.result)
        else
          Xor.left(DecodingFailure("CanBuildFrom for A", c.history))
    }

    override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[C[A]] = {
      val arrayCursor = c.downArray

      if (arrayCursor.succeeded) {
        arrayCursor.any.traverseDecodeAccumulating(Validated.valid(cbf.apply))(
          _.right,
          (acc, hcursor) => AccumulatingDecoder.resultInstance.map2(acc, d.decodeAccumulating(hcursor))(_ += _)
        ).map(_.result)
      } else if (c.focus.isArray)
          Validated.valid(cbf.apply.result)
        else
          Validated.invalidNel(DecodingFailure("CanBuildFrom for A", c.history))
    }
  }

  private[this] final val rightNone: Xor[DecodingFailure, Option[Nothing]] = Xor.right(None)

  /**
   * @group Decoding
   */
  implicit final def decodeOption[A](implicit d: Decoder[A]): Decoder[Option[A]] =
    withReattempt(c =>
      if (c.succeeded) {
        if (c.any.focus.isNull) rightNone else d(c.any) match {
          case Xor.Right(a) => Xor.right(Some(a))
          case Xor.Left(df) if df.history.isEmpty => rightNone
          case Xor.Left(df) => Xor.left(df)
        }
      } else if (!c.history.takeWhile(_.failed).exists(_.incorrectFocus)) rightNone else {
        Xor.left(DecodingFailure("[A]Option[A]", c.history))
      }
    )

  /**
   * @group Decoding
   */
  implicit final def decodeSome[A](implicit d: Decoder[A]): Decoder[Some[A]] = d.map(Some(_))

  /**
   * @group Decoding
   */
  implicit final val decodeNone: Decoder[None.type] = new Decoder[None.type] {
    final def apply(c: HCursor): Result[None.type] = if (c.focus.isNull) Xor.right(None) else {
      Xor.left(DecodingFailure("None", c.history))
    }
  }


  /**
   * @group Decoding
   */
  implicit final def decodeMapLike[M[K, +V] <: Map[K, V], K, V](implicit
    dk: KeyDecoder[K],
    dv: Decoder[V],
    cbf: CanBuildFrom[Nothing, (K, V), M[K, V]]
  ): Decoder[M[K, V]] = new Decoder[M[K, V]] {
    private[this] def failure(c: HCursor): DecodingFailure = DecodingFailure("[K, V]Map[K, V]", c.history)

    def apply(c: HCursor): Result[M[K, V]] = c.fields match {
      case None => Xor.left[DecodingFailure, M[K, V]](failure(c))
      case Some(fields) =>
        val builder = cbf()
        spinResult(fields, c, builder) match {
          case None => Xor.right(builder.result())
          case Some(error) => Xor.left(error)
        }
    }

    override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[M[K, V]] =
      c.fields match {
        case None => Validated.invalidNel(failure(c))
        case Some(fields) =>
          val builder = cbf()
          spinAccumulating(fields, c, builder, false, List.newBuilder[DecodingFailure]) match {
            case Nil => Validated.valid(builder.result())
            case error :: errors => Validated.invalid(NonEmptyList(error, errors))
          }
      }

    @scala.annotation.tailrec
    private[this] def spinResult(
      fields: List[String],
      c: HCursor,
      builder: Builder[(K, V), M[K, V]]
    ): Option[DecodingFailure] = fields match {
      case Nil => None
      case h :: t =>
        val atH = c.downField(h)

        atH.as(dv) match {
          case Xor.Left(error) => Some(error)
          case Xor.Right(value) => dk(h) match {
            case None => Some(failure(atH.any))
            case Some(k) =>
              builder += (k -> value)
              spinResult(t, c, builder)
          }
      }
    }

    @scala.annotation.tailrec
    private[this] def spinAccumulating(
      fields: List[String],
      c: HCursor,
      builder: Builder[(K, V), M[K, V]],
      failed: Boolean,
      errors: Builder[DecodingFailure, List[DecodingFailure]]
    ): List[DecodingFailure] = fields match {
      case Nil => errors.result
      case h :: t =>
        val atH = c.downField(h)

        (atH.as(dv), dk(h)) match {
          case (Xor.Left(error), _) => spinAccumulating(t, c, builder, true, errors += error)
          case (_, None) => spinAccumulating(t, c, builder, true, errors += failure(atH.any))
          case (Xor.Right(value), Some(k)) =>
            if (!failed) {
              builder += (k -> value)
            } else ()

            spinAccumulating(t, c, builder, failed, errors)
        }
    }
  }

  /**
   * @group Decoding
   */
  implicit final def decodeSet[A: Decoder]: Decoder[Set[A]] =
    decodeCanBuildFrom[A, List].map(_.toSet).withErrorMessage("[A]Set[A]")

  /**
    * @group Decoding
    */
  implicit final def decodeOneAnd[A, C[_]](implicit
  da: Decoder[A],
  cbf: CanBuildFrom[Nothing, A, C[A]]
  ): Decoder[OneAnd[C, A]] = new Decoder[OneAnd[C, A]] {
    def apply(c: HCursor): Result[OneAnd[C, A]] = {
      val arr = c.downArray
      for {
        head <- da.tryDecode(arr)
        tail <- decodeCanBuildFrom[A, C].tryDecode(arr.delete)
      } yield OneAnd(head, tail)
    }

    override private[circe] def decodeAccumulating(
      c: HCursor
    ): AccumulatingDecoder.Result[OneAnd[C, A]] = {
      val arr = c.downArray
      val head = da.tryDecodeAccumulating(arr)
      val tail = decodeCanBuildFrom[A, C].tryDecodeAccumulating(arr.delete)
      tail.ap(head.map(h => (t: C[A]) => OneAnd(h, t)))
    }
  }

  /**
   * @group Disjunction
   */
  final def decodeXor[A, B](leftKey: String, rightKey: String)(implicit
    da: Decoder[A],
    db: Decoder[B]
  ): Decoder[Xor[A, B]] = new Decoder[Xor[A, B]] {
    final def apply(c: HCursor): Result[Xor[A, B]] = {
      val l = c.downField(leftKey)
      val r = c.downField(rightKey)

      if (l.succeeded && !r.succeeded) {
        da(l.any).map(Xor.left(_))
      } else if (!l.succeeded && r.succeeded) {
        db(r.any).map(Xor.right(_))
      } else Xor.left(DecodingFailure("[A, B]Xor[A, B]", c.history))
    }
  }

  /**
   * @group Disjunction
   */
  final def decodeEither[A, B](leftKey: String, rightKey: String)(implicit
    da: Decoder[A],
    db: Decoder[B]
  ): Decoder[Either[A, B]] =
    decodeXor[A, B](leftKey, rightKey).map(_.toEither).withErrorMessage("[A, B]Either[A, B]")

  /**
   * @group Disjunction
   */
  final def decodeValidated[E, A](failureKey: String, successKey: String)(implicit
    de: Decoder[E],
    da: Decoder[A]
  ): Decoder[Validated[E, A]] =
    decodeXor[E, A](
      failureKey,
      successKey
    ).map(_.toValidated).withErrorMessage("[E, A]Validated[E, A]")

  /**
   * @group Instances
   */
  implicit final val monadInstances: SemigroupK[Decoder] with MonadError[Decoder, DecodingFailure] =
    new SemigroupK[Decoder] with MonadError[Decoder, DecodingFailure] {
      final def combineK[A](x: Decoder[A], y: Decoder[A]): Decoder[A] = x.or(y)
      final def pure[A](a: A): Decoder[A] = new Decoder[A] {
        final def apply(c: HCursor): Result[A] = Xor.right(a)
        override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] = Validated.valid(a)
      }
      override final def map[A, B](fa: Decoder[A])(f: A => B): Decoder[B] = fa.map(f)
      override final def product[A, B](fa: Decoder[A], fb: Decoder[B]): Decoder[(A, B)] = fa.and(fb)
      final def flatMap[A, B](fa: Decoder[A])(f: A => Decoder[B]): Decoder[B] = fa.flatMap(f)

      final def raiseError[A](e: DecodingFailure): Decoder[A] = Decoder.failed(e)
      final def handleErrorWith[A](fa: Decoder[A])(f: DecodingFailure => Decoder[A]): Decoder[A] = fa.handleErrorWith(f)
    }
}

private[circe] trait LowPriorityDecoders {
  implicit def importedDecoder[A](implicit exported: Exported[Decoder[A]]): Decoder[A] = exported.instance
}
