package io.circe

import cats.Monad
import cats.data.{ Kleisli, NonEmptyList, Validated, Xor }
import cats.std.list._
import cats.syntax.functor._
import cats.syntax.monoidal._
import java.util.UUID
import scala.collection.generic.CanBuildFrom
import scala.collection.mutable

trait Decoder[A] extends Serializable { self =>
  type Config

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

  private[circe] def tryDecodeAccumulating(c: ACursor): AccumulatingDecoder.Result[A] =
    if (c.succeeded) decodeAccumulating(c.any) else Validated.invalidNel(
      DecodingFailure("Attempt to decode value on failed cursor", c.history)
    )

  /**
   * Decode the given [[Json]] value.
   */
  final def decodeJson(j: Json): Decoder.Result[A] = apply(j.cursor.hcursor)

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

  final def ap[B](f: Decoder[A => B]): Decoder[B] = new Decoder[B] {
    final def apply(c: HCursor): Decoder.Result[B] = self(c).flatMap(a => f(c).map(_(a)))
    override def tryDecode(c: ACursor): Decoder.Result[B] =
      self.tryDecode(c).flatMap(a => f.tryDecode(c).map(_(a)))

    override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[B] =
      self.decodeAccumulating(c).ap(f.decodeAccumulating(c))
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
  final def validate(pred: HCursor => Boolean, message: => String): Decoder[A] =
    Decoder.instance(c =>
      if (pred(c)) apply(c) else Xor.left(DecodingFailure(message, c.history))
    )

  /**
   * Convert to a Kleisli arrow.
   */
  final def kleisli: Kleisli[({ type L[x] = Decoder.Result[x] })#L, HCursor, A] =
    Kleisli[({ type L[x] = Decoder.Result[x] })#L, HCursor, A](apply(_))

  /**
   * Combine two decoders.
   */
  final def and[B](fb: Decoder[B]): Decoder[(A, B)] = new Decoder[(A, B)] {
    final def apply(c: HCursor): Decoder.Result[(A, B)] = (self(c) |@| fb(c)).tupled
    override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[(A, B)] =
      (self.decodeAccumulating(c) |@| fb.decodeAccumulating(c)).tupled
  }

  /**
   * Combine two decoders.
   */
  @deprecated("Use and", "0.3.0")
  final def &&&[B](fb: Decoder[B]): Decoder[(A, B)] = and(fb)

  /**
   * Choose the first succeeding decoder.
   */
  final def or[AA >: A](d: => Decoder[AA]): Decoder[AA] = Decoder.instance[AA] { c =>
    val res = apply(c).map(a => (a: AA))
    res.fold(_ => d(c), _ => res)
  }

  /**
   * Choose the first succeeding decoder.
   */
  @deprecated("Use or", "0.3.0")
  final def |||[AA >: A](d: => Decoder[AA]): Decoder[AA] = or(d)

  /**
   * Run one or another decoder.
   */
  final def split[B](d: Decoder[B]): Xor[HCursor, HCursor] => Decoder.Result[Xor[A, B]] = _.fold(
    c => this(c).map(Xor.left),
    c => d(c).map(Xor.right)
  )

  /**
   * Run two decoders.
   */
  final def product[B](x: Decoder[B]): (HCursor, HCursor) => Decoder.Result[(A, B)] = (a1, a2) =>
    (this(a1) |@| x(a2)).tupled

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
  final def emap[B](f: A => Xor[String, B]): Decoder[B] = Decoder.instance(c =>
    self(c).flatMap(a => f(a).leftMap(message => DecodingFailure(message, c.history)))
  )
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
 * instances are implicit, since they require non-obvious decisions about the names of the
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
 * @author Travis Brown
 */
final object Decoder extends TupleDecoders with LowPriorityDecoders {
  import Json._

  final type Result[A] = Xor[DecodingFailure, A]

  /**
   * Return an instance for a given type.
   *
   * @group Utilities
   */
  final def apply[A](implicit d: Decoder[A]): Decoder[A] = d

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
   * Construct an instance that always fails with the given error message.
   *
   * @group Utilities
   */
  final def failWith[A](message: String): Decoder[A] = instance(_ =>
    Xor.left(DecodingFailure(message, Nil))
  )

  /**
   * @group Decoding
   */
  implicit final val decodeHCursor: Decoder[HCursor] = instance(Xor.right)

  /**
   * @group Decoding
   */
  implicit final val decodeJson: Decoder[Json] = instance(c => Xor.right(c.focus))

  /**
   * @group Decoding
   */
  implicit final val decodeJsonObject: Decoder[JsonObject] =
    instance(c => Xor.fromOption(c.focus.asObject, DecodingFailure("JsonObject", c.history)))

  /**
   * @group Decoding
   */
  implicit final val decodeJsonNumber: Decoder[JsonNumber] =
    instance(c => Xor.fromOption(c.focus.asNumber, DecodingFailure("JsonNumber", c.history)))

  /**
   * @group Decoding
   */
  implicit final val decodeString: Decoder[String] = instance { c =>
    c.focus match {
      case JString(string) => Xor.right(string)
      case _ => Xor.left(DecodingFailure("String", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit final val decodeUnit: Decoder[Unit] = instance { c =>
    c.focus match {
      case JNull => Xor.right(())
      case JObject(obj) if obj.isEmpty => Xor.right(())
      case JArray(arr) if arr.isEmpty => Xor.right(())
      case _ => Xor.left(DecodingFailure("String", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit final val decodeBoolean: Decoder[Boolean] = instance { c =>
    c.focus match {
      case JBoolean(b) => Xor.right(b)
      case _ => Xor.left(DecodingFailure("Boolean", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit final val decodeChar: Decoder[Char] = instance { c =>
    c.focus match {
      case JString(string) if string.length == 1 => Xor.right(string.charAt(0))
      case _ => Xor.left(DecodingFailure("Char", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit final val decodeFloat: Decoder[Float] = instance { c =>
    c.focus match {
      case JNull => Xor.right(Float.NaN)
      case JNumber(number) => Xor.right(number.toDouble.toFloat)
      case _ => Xor.left(DecodingFailure("Float", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit final val decodeDouble: Decoder[Double] = instance { c =>
    c.focus match {
      case JNull => Xor.right(Double.NaN)
      case JNumber(number) => Xor.right(number.toDouble)
      case _ => Xor.left(DecodingFailure("Double", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit final val decodeByte: Decoder[Byte] = instance { c =>
    c.focus match {
      case JNumber(number) => Xor.right(number.truncateToByte)
      case JString(string) => try {
        Xor.right(string.toByte)
      } catch {
        case _: NumberFormatException => Xor.left(DecodingFailure("Byte", c.history))
      }
      case _ => Xor.left(DecodingFailure("Byte", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit final val decodeShort: Decoder[Short] = instance { c =>
    c.focus match {
      case JNumber(number) => Xor.right(number.truncateToShort)
      case JString(string) => try {
        Xor.right(string.toShort)
      } catch {
        case _: NumberFormatException => Xor.left(DecodingFailure("Short", c.history))
      }
      case _ => Xor.left(DecodingFailure("Short", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit final val decodeInt: Decoder[Int] = instance { c =>
    c.focus match {
      case JNumber(number) => Xor.right(number.truncateToInt)
      case JString(string) => try {
        Xor.right(string.toInt)
      } catch {
        case _: NumberFormatException => Xor.left(DecodingFailure("Int", c.history))
      }
      case _ => Xor.left(DecodingFailure("Int", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit final val decodeLong: Decoder[Long] = instance { c =>
    c.focus match {
      case JNumber(number) => Xor.right(number.truncateToLong)
      case JString(string) => try {
        Xor.right(string.toLong)
      } catch {
        case _: NumberFormatException => Xor.left(DecodingFailure("Long", c.history))
      }
      case _ => Xor.left(DecodingFailure("Long", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit final val decodeBigInt: Decoder[BigInt] = instance { c =>
    c.focus match {
      case JNumber(number) => Xor.right(number.truncateToBigInt)
      case JString(string) => try {
        Xor.right(BigInt(string))
      } catch {
        case _: NumberFormatException => Xor.left(DecodingFailure("BigInt", c.history))
      }
      case _ => Xor.left(DecodingFailure("BigInt", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit final val decodeBigDecimal: Decoder[BigDecimal] = instance { c =>
    c.focus match {
      case JNumber(number) => Xor.right(number.toBigDecimal)
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
  implicit final val decodeUUID: Decoder[UUID] = instance { c =>
    c.focus match {
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
          (acc, hcursor) => hcursor.as[A].map(acc += _)
        ).map(_.result)
      } else if (c.focus.isArray)
          Xor.right(cbf.apply.result)
        else
          Xor.left(DecodingFailure("CanBuildFrom for A", c.history))
    }

    override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[C[A]] =
      c.downArray.success.fold(
        if (c.focus.isArray)
          Validated.valid(cbf.apply.result)
        else
          Validated.invalidNel(DecodingFailure("CanBuildFrom for A", c.history))
      )(
        _.traverseDecodeAccumulating(Validated.valid(cbf.apply))(
          _.right,
          (acc, hcursor) => d.decodeAccumulating(hcursor).ap(
            acc.map(builder => (a: A) => builder += a)
          )
        ).map(_.result)
      )
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
      } else rightNone
    )

  /**
   * @group Decoding
   */
  implicit final def decodeMap[M[K, +V] <: Map[K, V], V](implicit
    d: Decoder[V],
    cbf: CanBuildFrom[Nothing, (String, V), M[String, V]]
  ): Decoder[M[String, V]] = new Decoder[M[String, V]] {
    def apply(c: HCursor): Decoder.Result[M[String, V]] =
      c.fields.fold(
        Xor.left[DecodingFailure, M[String, V]](DecodingFailure("[V]Map[String, V]", c.history))
      ) { s =>
        val builder = cbf()
        spinResult(s, c, builder).fold[Result[M[String, V]]](Xor.right(builder.result()))(Xor.left)
      }

    override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[M[String, V]] =
      c.fields.fold[AccumulatingDecoder.Result[M[String, V]]](
        Validated.invalid(DecodingFailure("[V]Map[String, V]", c.history)).toValidatedNel
      ) { s =>
        val builder = cbf()
        spinAccumulating(s, c, builder, false, List.newBuilder[DecodingFailure]) match {
          case Nil => Validated.valid(builder.result())
          case error :: errors => Validated.invalid(NonEmptyList(error, errors))
        }
      }

    @scala.annotation.tailrec
    private[this] def spinResult(
      x: List[String],
      c: HCursor,
      builder: mutable.Builder[(String, V), M[String, V]]
    ): Option[DecodingFailure] =
      x match {
        case Nil => None
        case h :: t =>
          c.get(h)(d) match {
            case Xor.Left(error) => Some(error)
            case Xor.Right(value) =>
              builder += (h -> value)
              spinResult(t, c, builder)
          }
      }

    @scala.annotation.tailrec
    private[this] def spinAccumulating(
      x: List[String],
      c: HCursor,
      builder: mutable.Builder[(String, V), M[String, V]],
      failed: Boolean,
      errors: mutable.Builder[DecodingFailure, List[DecodingFailure]]
    ): List[DecodingFailure] =
      x match {
        case Nil => errors.result
        case h :: t =>
          c.get(h)(d) match {
            case Xor.Left(error) => spinAccumulating(t, c, builder, true, errors += error)
            case Xor.Right(value) =>
              if (!failed) {
                builder += (h -> value)
              }
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
  implicit final def decodeNonEmptyList[A: Decoder]: Decoder[NonEmptyList[A]] =
    decodeCanBuildFrom[A, List].flatMap { l =>
      instance { c =>
        l match {
          case h :: t => Xor.right(NonEmptyList(h, t))
          case Nil => Xor.left(DecodingFailure("[A]NonEmptyList[A]", c.history))
        }
      }
    }.withErrorMessage("[A]NonEmptyList[A]")

  /**
   * @group Disjunction
   */
  final def decodeXor[A, B](leftKey: String, rightKey: String)(implicit
    da: Decoder[A],
    db: Decoder[B]
  ): Decoder[Xor[A, B]] = instance { c =>
    val l = c.downField(leftKey)
    val r = c.downField(rightKey)

    if (l.succeeded && !r.succeeded) {
      da(l.any).map(Xor.left(_))
    } else if (!l.succeeded && r.succeeded) {
      db(r.any).map(Xor.right(_))
    } else Xor.left(DecodingFailure("[A, B]Xor[A, B]", c.history))
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
  implicit final val monadDecode: Monad[Decoder] = new Monad[Decoder] {
    final def pure[A](a: A): Decoder[A] = instance(_ => Xor.right(a))
    override final def map[A, B](fa: Decoder[A])(f: A => B): Decoder[B] = fa.map(f)
    override final def ap[A, B](fa: Decoder[A])(f: Decoder[A => B]): Decoder[B] = fa.ap(f)
    override final def product[A, B](fa: Decoder[A], fb: Decoder[B]): Decoder[(A, B)] = fa.and(fb)
    final def flatMap[A, B](fa: Decoder[A])(f: A => Decoder[B]): Decoder[B] = fa.flatMap(f)
  }
}

@export.imports[Decoder]
private[circe] trait LowPriorityDecoders

trait ConfiguredDecoder[C, A] extends Decoder[A] {
  final type Config = C
}

@export.imports[ConfiguredDecoder]
final object ConfiguredDecoder
