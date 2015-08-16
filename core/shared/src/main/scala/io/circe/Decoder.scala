package io.circe

import cats.Monad
import cats.data.{ Kleisli, NonEmptyList, Validated, Xor }

import scala.collection.generic.CanBuildFrom

trait Decoder[A] { self =>
  /**
   * Decode the given hcursor.
   */
  def apply(c: HCursor): Decoder.Result[A]

  /**
   * Decode the given acursor.
   */
  def tryDecode(c: ACursor): Decoder.Result[A] = c.either.fold(
    invalid =>
      Xor.left(DecodingFailure("Attempt to decode value on failed cursor", invalid.history)),
    apply
  )

  /**
   * Decode the given [[Json]] value.
   */
  def decodeJson(j: Json): Decoder.Result[A] = apply(j.cursor.hcursor)

  /**
   * Map a function over this [[Decoder]].
   */
  def map[B](f: A => B): Decoder[B] = new Decoder[B] {
    def apply(c: HCursor): Decoder.Result[B] = self(c).map(f)
    override def tryDecode(c: ACursor): Decoder.Result[B] = self.tryDecode(c).map(f)
  }

  /**
   * Monadically bind a function over this [[Decoder]].
   */
  def flatMap[B](f: A => Decoder[B]): Decoder[B] = new Decoder[B] {
    def apply(c: HCursor): Decoder.Result[B] = {
      self(c).flatMap(a => f(a)(c))
    }

    override def tryDecode(c: ACursor): Decoder.Result[B] = {
      self.tryDecode(c).flatMap(a => f(a).tryDecode(c))
    }
  }

  /**
   * Build a new instance with the specified error message.
   */
  def withErrorMessage(message: String): Decoder[A] = Decoder.instance(c =>
    apply(c).leftMap(_.withMessage(message))
  )

  /**
   * Build a new instance that fails if the condition does not hold.
   */
  def validate(pred: HCursor => Boolean, message: => String): Decoder[A] = Decoder.instance(c =>
    if (pred(c)) apply(c) else Xor.left(DecodingFailure(message, c.history))
  )

  /**
   * Convert to a Kleisli arrow.
   */
  def kleisli: Kleisli[({ type L[x] = Decoder.Result[x] })#L, HCursor, A] =
    Kleisli[({ type L[x] = Decoder.Result[x] })#L, HCursor, A](apply(_))

  /**
   * Combine two decoders.
   */
  def &&&[B](x: Decoder[B]): Decoder[(A, B)] = Decoder.instance(c =>
    for {
      a <- this(c)
      b <- x(c)
    } yield (a, b)
  )

  /**
   * Choose the first succeeding decoder.
   */
  def |||[AA >: A](d: => Decoder[AA]): Decoder[AA] = Decoder.instance[AA] { c =>
    val res = apply(c).map(a => (a: AA))
    res.fold(_ => d(c), _ => res)
  }

  /**
   * Run one or another decoder.
   */
  def split[B](d: Decoder[B]): Xor[HCursor, HCursor] => Decoder.Result[Xor[A, B]] = _.fold(
    c => this(c).map(Xor.left),
    c => d(c).map(Xor.right)
  )

  /**
   * Run two decoders.
   */
  def product[B](x: Decoder[B]): (HCursor, HCursor) => Decoder.Result[(A, B)] = (a1, a2) =>
    for {
      a <- this(a1)
      b <- x(a2)
    } yield (a, b)
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
 * @groupprio Disjunction 8
 *
 * @groupname Instances Type class instances
 * @groupprio Instances 10
 *
 * @author Travis Brown
 */
object Decoder {
  import Json._

  type Result[A] = Xor[DecodingFailure, A]

  /**
   * A wrapper that supports proper prioritization of derived instances.
   *
   * @group Utilities
   */
  class Secondary[A](val value: Decoder[A]) extends AnyVal

  /**
   * Return an instance for a given type.
   *
   * @group Utilities
   */
  def apply[A](implicit d: Decoder[A]): Decoder[A] = d

  /**
   * Construct an instance from a function.
   *
   * @group Utilities
   */
  def instance[A](f: HCursor => Result[A]): Decoder[A] = new Decoder[A] {
    def apply(c: HCursor): Result[A] = f(c)
  }

  /**
   * Construct an instance from a function that will reattempt on failure.
   *
   * @group Utilities
   */
  def withReattempt[A](f: ACursor => Result[A]): Decoder[A] = new Decoder[A] {
    def apply(c: HCursor): Result[A] = tryDecode(c.acursor)

    override def tryDecode(c: ACursor) = f(c)
  }

  /**
   * Unwrap a [[Secondary]] wrapper.
   *
   * @group Decoding
   */
  implicit def fromSecondaryDecoder[A](implicit d: Secondary[A]): Decoder[A] = d.value

  /**
   * @group Decoding
   */
  implicit val decodeHCursor: Decoder[HCursor] = instance(Xor.right)

  /**
   * @group Decoding
   */
  implicit val decodeJson: Decoder[Json] = instance(c => Xor.right(c.focus))

  /**
   * @group Decoding
   */
  implicit val decodeString: Decoder[String] = instance { c =>
    c.focus match {
      case JString(string) => Xor.right(string)
      case _ => Xor.left(DecodingFailure("String", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit val decodeUnit: Decoder[Unit] = instance { c =>
    c.focus match {
      case JNull => Xor.right(())
      case JObject(obj) if obj.isEmpty => Xor.right(())
      case JArray(arr) if arr.length == 0 => Xor.right(())
      case _ => Xor.left(DecodingFailure("String", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit val decodeBoolean: Decoder[Boolean] = instance { c =>
    c.focus match {
      case JBoolean(b) => Xor.right(b)
      case _ => Xor.left(DecodingFailure("Boolean", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit val decodeChar: Decoder[Char] = instance { c =>
    c.focus match {
      case JString(string) if string.length == 1 => Xor.right(string.charAt(0))
      case _ => Xor.left(DecodingFailure("Char", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit val decodeFloat: Decoder[Float] = instance { c =>
    c.focus match {
      case JNull => Xor.right(Float.NaN)
      case JNumber(number) => Xor.right(number.toDouble.toFloat)
      case _ => Xor.left(DecodingFailure("Float", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit val decodeDouble: Decoder[Double] = instance { c =>
    c.focus match {
      case JNull => Xor.right(Double.NaN)
      case JNumber(number) => Xor.right(number.toDouble)
      case _ => Xor.left(DecodingFailure("Float", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit val decodeByte: Decoder[Byte] = instance { c =>
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
  implicit val decodeShort: Decoder[Short] = instance { c =>
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
  implicit val decodeInt: Decoder[Int] = instance { c =>
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
  implicit val decodeLong: Decoder[Long] = instance { c =>
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
  implicit val decodeBigInt: Decoder[BigInt] = instance { c =>
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
  implicit val decodeBigDecimal: Decoder[BigDecimal] = instance { c =>
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
  implicit def decodeCanBuildFrom[A, C[_]](implicit
    d: Decoder[A],
    cbf: CanBuildFrom[Nothing, A, C[A]]
  ): Decoder[C[A]] = instance { c =>
    c.downArray.success.fold(
      if (c.focus.isArray)
        Xor.right(cbf.apply.result)
      else
        Xor.left(DecodingFailure("CanBuildFrom for A", c.history))
    )(
      _.traverseDecode(cbf.apply)(
        _.right,
        (acc, hcursor) => hcursor.as[A].map(acc += _)
      ).map(_.result)
    )
  }

  /**
   * @group Decoding
   */
  implicit def decodeOption[A](implicit d: Decoder[A]): Decoder[Option[A]] =
    withReattempt { a =>
      a.success.fold[Result[Option[A]]](Xor.right(None)) { valid =>
        if (valid.focus.isNull) Xor.right(None) else d(valid).fold[Result[Option[A]]](
          df =>
            df.history.headOption.fold[Result[Option[A]]](
              Xor.right(None)
            )(_ => Xor.left(df)),
          a => Xor.right(Some(a))
        )
      }
    }

  /**
   * @group Decoding
   */
  implicit def decodeMap[M[K, +V] <: Map[K, V], V](implicit
    d: Decoder[V],
    cbf: CanBuildFrom[Nothing, (String, V), M[String, V]]
  ): Decoder[M[String, V]] = instance { c =>
    c.fields.fold(
      Xor.left[DecodingFailure, M[String, V]](DecodingFailure("[V]Map[String, V]", c.history))
    ) { s =>
      @scala.annotation.tailrec
      def spin(
        x: List[String],
        acc: Result[Vector[(String, V)]]
      ): Result[M[String, V]] =
        x match {
          case Nil =>
            acc.map { fields =>
              (cbf() ++= fields).result()
            }
          case h :: t =>
            val acc0 = for {
              m <- acc
              v <- c.get(h)(d)
            } yield m :+ (h -> v)

            if (acc0.isLeft) spin(Nil, acc0)
            else spin(t, acc0)
        }
      spin(s, Xor.right(Vector.empty))
    }
  }

  /**
   * @group Decoding
   */
  implicit def decodeSet[A: Decoder]: Decoder[Set[A]] =
    decodeCanBuildFrom[A, List].map(_.toSet).withErrorMessage("[A]Set[A]")

  /**
   * @group Decoding
   */
  implicit def decodeNonEmptyList[A: Decoder]: Decoder[NonEmptyList[A]] =
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
  def decodeXor[A, B](leftKey: String, rightKey: String)(implicit
    da: Decoder[A],
    db: Decoder[B]
  ): Decoder[Xor[A, B]] = instance { c =>
    val l = (c.downField(leftKey)).success
    val r = (c.downField(rightKey)).success

    (l, r) match {
      case (Some(hcursor), None) => da(hcursor).map(Xor.left(_))
      case (None, Some(hcursor)) => db(hcursor).map(Xor.right(_))
      case _ => Xor.left(DecodingFailure("[A, B]Xor[A, B]", c.history))
    }
  }

  /**
   * @group Disjunction
   */
  def decodeEither[A, B](leftKey: String, rightKey: String)(implicit
    da: Decoder[A],
    db: Decoder[B]
  ): Decoder[Either[A, B]] =
    decodeXor[A, B](leftKey, rightKey).map(_.toEither).withErrorMessage("[A, B]Either[A, B]")

  /**
   * @group Disjunction
   */
  def decodeValidated[E, A](failureKey: String, successKey: String)(implicit
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
  implicit val monadDecode: Monad[Decoder] = new Monad[Decoder] {
    def pure[A](a: A): Decoder[A] = instance(_ => Xor.right(a))
    def flatMap[A, B](fa: Decoder[A])(f: A => Decoder[B]): Decoder[B] = fa.flatMap(f)
  }
}
