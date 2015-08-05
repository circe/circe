package io.jfc

import cats.Monad
import cats.data.{ Kleisli, NonEmptyList, Validated, Xor }

import scala.collection.generic.CanBuildFrom

trait Decode[A] { self =>
  /**
   * Decode the given hcursor.
   */
  def apply(c: HCursor): Xor[DecodeFailure, A]

  /**
   * Decode the given acursor.
   */
  def tryDecode(c: ACursor): Xor[DecodeFailure, A] = c.either.fold(
    invalid =>
      Xor.left(DecodeFailure("Attempt to decode value on failed cursor", invalid.history)),
    apply
  )

  /**
   * Decode the given json.
   */
  def decodeJson(j: Json): Xor[DecodeFailure, A] = apply(j.cursor.hcursor)

  /**
   * Map a function over this .
   */
  def map[B](f: A => B): Decode[B] = new Decode[B] {
    def apply(c: HCursor): Xor[DecodeFailure, B] = self(c).map(f)
    override def tryDecode(c: ACursor): Xor[DecodeFailure, B] = self.tryDecode(c).map(f)
  }

  /**
   * Monadic bind.
   */
  def flatMap[B](f: A => Decode[B]): Decode[B] = new Decode[B] {
    def apply(c: HCursor): Xor[DecodeFailure, B] = {
      self(c).flatMap(a => f(a)(c))
    }

    override def tryDecode(c: ACursor): Xor[DecodeFailure, B] = {
      self.tryDecode(c).flatMap(a => f(a).tryDecode(c))
    }
  }

  /**
   * Build a new instance with the specified error message.
   */
  def withErrorMessage(message: String): Decode[A] = Decode.instance(c =>
    apply(c).leftMap(_.withMessage(message))
  )

  /**
   * Build a new instance that fails if the condition does not hold.
   */
  def validate(pred: HCursor => Boolean, message: => String): Decode[A] = Decode.instance(c =>
    if (pred(c)) apply(c) else Xor.left(DecodeFailure(message, c.history))
  )

  /**
   * Convert to a kleisli arrow.
   */
  def kleisli: Kleisli[({ type L[x] = Xor[DecodeFailure, x] })#L, HCursor, A] =
    Kleisli[({ type L[x] = Xor[DecodeFailure, x] })#L, HCursor, A](apply(_))

  /**
   * Combine two decoders.
   */
  def &&&[B](x: Decode[B]): Decode[(A, B)] = Decode.instance(c =>
    for {
      a <- this(c)
      b <- x(c)
    } yield (a, b)
  )

  /**
   * Choose the first succeeding decoder.
   */
  def |||[AA >: A](d: => Decode[AA]): Decode[AA] = Decode.instance[AA] { c =>
    val res = apply(c).map(a => (a: AA))
    res.fold(_ => d(c), _ => res)
  }

  /**
   * Run one or another decoder.
   */
  def split[B](d: Decode[B]): Xor[HCursor, HCursor] => Xor[DecodeFailure, Xor[A, B]] = _.fold(
    c => this(c).map(Xor.left),
    c => d(c).map(Xor.right)
  )

  /**
   * Run two decoders.
   */
  def product[B](x: Decode[B]): (HCursor, HCursor) => Xor[DecodeFailure, (A, B)] = (a1, a2) =>
    for {
      a <- this(a1)
      b <- x(a2)
    } yield (a, b)
}

/**
 * Utilities and instances for [[Decode]].
 *
 * @groupname Utilities Miscellaneous utilities
 * @groupprio Utilities 0
 *
 * @groupname Decode Decode instances
 * @groupprio Decode 2
 *
 * @groupname Disjunction Disjunction instances
 * @groupdesc Disjunction Instance creation methods for disjunction-like types.
 * Note that these are implicit, since they require non-obvious decisions about
 * the names of the discriminators. If you want instances for these types you
 * can include the following import in your program:
 * {{{
 *    import io.jfc.disjunctionCodecs._
 * }}}
 * @groupprio Disjunction 8
 *
 * @groupname Instances Type class instances
 * @groupprio Instances 10
 *
 * @author Travis Brown
 */
object Decode {
  import Json._

  /**
   * Return an instance for a given type.
   *
   * @group Utilities
   */
  def apply[A](implicit d: Decode[A]): Decode[A] = d

  /**
   * Construct an instance from a function.
   *
   * @group Utilities
   */
  def instance[A](f: HCursor => Xor[DecodeFailure, A]): Decode[A] = new Decode[A] {
    def apply(c: HCursor): Xor[DecodeFailure, A] = f(c)
  }

  /**
   * Construct an instance from a function that will reattempt on failure.
   *
   * @group Utilities
   */
  def withReattempt[A](f: ACursor => Xor[DecodeFailure, A]): Decode[A] = new Decode[A] {
    def apply(c: HCursor): Xor[DecodeFailure, A] = tryDecode(c.acursor)

    override def tryDecode(c: ACursor) = f(c)
  }

  /**
   * @group Decode
   */
  implicit def fromCodec[A](implicit c: Codec[A]): Decode[A] = c.decoder

  private[this] def partialDecoder[A](message: String)(f: PartialFunction[Json, A]): Decode[A] =
    instance(c =>
      f.andThen(Xor.right[DecodeFailure, A]).applyOrElse(
        c.focus,
        (_: Json) => Xor.left(DecodeFailure(message, c.history))
      )
    )

  /**
   * @group Decode
   */
  implicit val decodeHCursor: Decode[HCursor] = instance(Xor.right)

  /**
   * @group Decode
   */
  implicit val decodeJson: Decode[Json] = instance(c => Xor.right(c.focus))

  /**
   * @group Decode
   */
  implicit val decodeString: Decode[String] = instance { c =>
    c.focus match {
      case JString(string) => Xor.right(string)
      case _ => Xor.left(DecodeFailure("String", c.history))
    }
  }

  /**
   * @group Decode
   */
  implicit val decodeUnit: Decode[Unit] = partialDecoder("Unit") {
    case JNull => Xor.right(())
    case JObject(obj) if obj.isEmpty => Xor.right(())
    case JArray(arr) if arr.length == 0 => Xor.right(())
  }

  /**
   * @group Decode
   */
  implicit val decodeBoolean: Decode[Boolean] = partialDecoder("Boolean") {
    case JBool(boolean) => boolean
  }

  /**
   * @group Decode
   */
  implicit val decodeChar: Decode[Char] = instance { c =>
    c.focus match {
      case JString(string) if string.length == 1 => Xor.right(string.charAt(0))
      case _ => Xor.left(DecodeFailure("Char", c.history))
    }
  }

  /**
   * @group Decode
   */
  implicit val decodeFloat: Decode[Float] = partialDecoder("String") {
    case JNull => Float.NaN
    case JNumber(number) => number.toDouble.toFloat
  }

  /**
   * @group Decode
   */
  implicit val decodeDouble: Decode[Double] = partialDecoder("String") {
    case JNull => Double.NaN
    case JNumber(number) => number.toDouble
  }

  /**
   * @group Decode
   */
  implicit val decodeByte: Decode[Byte] = instance { c =>
    c.focus match {
      case JNumber(number) => Xor.right(number.truncateToByte)
      case JString(string) => try {
        Xor.right(string.toByte)
      } catch {
        case _: NumberFormatException => Xor.left(DecodeFailure("Byte", c.history))
      }
      case _ => Xor.left(DecodeFailure("Byte", c.history))
    }
  }

  /**
   * @group Decode
   */
  implicit val decodeShort: Decode[Short] = instance { c =>
    c.focus match {
      case JNumber(number) => Xor.right(number.truncateToShort)
      case JString(string) => try {
        Xor.right(string.toShort)
      } catch {
        case _: NumberFormatException => Xor.left(DecodeFailure("Short", c.history))
      }
      case _ => Xor.left(DecodeFailure("Short", c.history))
    }
  }

  /**
   * @group Decode
   */
  implicit val decodeInt: Decode[Int] = instance { c =>
    c.focus match {
      case JNumber(number) => Xor.right(number.truncateToInt)
      case JString(string) => try {
        Xor.right(string.toInt)
      } catch {
        case _: NumberFormatException => Xor.left(DecodeFailure("Int", c.history))
      }
      case _ => Xor.left(DecodeFailure("Int", c.history))
    }
  }

  /**
   * @group Decode
   */
  implicit val decodeLong: Decode[Long] = instance { c =>
    c.focus match {
      case JNumber(number) => Xor.right(number.truncateToLong)
      case JString(string) => try {
        Xor.right(string.toLong)
      } catch {
        case _: NumberFormatException => Xor.left(DecodeFailure("Long", c.history))
      }
      case _ => Xor.left(DecodeFailure("Long", c.history))
    }
  }

  /**
   * @group Decode
   */
  implicit val decodeBigInt: Decode[BigInt] = instance { c =>
    c.focus match {
      case JNumber(number) => Xor.right(number.truncateToBigInt)
      case JString(string) => try {
        Xor.right(BigInt(string))
      } catch {
        case _: NumberFormatException => Xor.left(DecodeFailure("BigInt", c.history))
      }
      case _ => Xor.left(DecodeFailure("BigInt", c.history))
    }
  }

  /**
   * @group Decode
   */
  implicit val decodeBigDecimal: Decode[BigDecimal] = instance { c =>
    c.focus match {
      case JNumber(number) => Xor.right(number.toBigDecimal)
      case JString(string) => try {
        Xor.right(BigDecimal(string))
      } catch {
        case _: NumberFormatException => Xor.left(DecodeFailure("BigDecimal", c.history))
      }
      case _ => Xor.left(DecodeFailure("BigDecimal", c.history))
    }
  }

  /**
   * @group Decode
   */
  implicit def decodeCanBuildFrom[A, C[_]](implicit
    d: Decode[A],
    cbf: CanBuildFrom[Nothing, A, C[A]]
  ): Decode[C[A]] = instance { c =>
    c.downArray.success.fold(
      if (c.focus.isArray)
        Xor.right(cbf.apply.result)
      else
        Xor.left(DecodeFailure("CanBuildFrom for A", c.history))
    )(
      _.traverseDecode(cbf.apply)(
        _.right,
        (acc, hcursor) => hcursor.as[A].map(acc += _)
      ).map(_.result)
    )
  }

  /**
   * @group Decode
   */
  implicit def decodeOption[A](implicit d: Decode[A]): Decode[Option[A]] =
    withReattempt { a =>
      a.success.fold[Xor[DecodeFailure, Option[A]]](Xor.right(None)) { valid =>
        if (valid.focus.isNull) Xor.right(None) else d(valid).fold[Xor[DecodeFailure, Option[A]]](
          df =>
            df.history.headOption.fold[Xor[DecodeFailure, Option[A]]](
              Xor.right(None)
            )(_ => Xor.left(df)),
          a => Xor.right(Some(a))
        )
      }
    }

  /**
   * @group Decode
   */
  implicit def decodeMap[M[K, +V] <: Map[K, V], V](implicit
    d: Decode[V],
    cbf: CanBuildFrom[Nothing, (String, V), M[String, V]]
  ): Decode[M[String, V]] = instance { c =>
    c.fields.fold(
      Xor.left[DecodeFailure, M[String, V]](DecodeFailure("[V]Map[String, V]", c.history))
    ) { s =>
      @scala.annotation.tailrec
      def spin(
        x: List[String],
        acc: Xor[DecodeFailure, Vector[(String, V)]]
      ): Xor[DecodeFailure, M[String, V]] =
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
   * @group Decode
   */
  implicit def decodeSet[A: Decode]: Decode[Set[A]] =
    decodeCanBuildFrom[A, List].map(_.toSet).withErrorMessage("[A]Set[A]")

  /**
   * @group Decode
   */
  implicit def decodeNonEmptyList[A: Decode]: Decode[NonEmptyList[A]] =
    decodeCanBuildFrom[A, List].flatMap { l =>
      instance { c =>
        l match {
          case h :: t => Xor.right(NonEmptyList(h, t))
          case Nil => Xor.left(DecodeFailure("[A]NonEmptyList[A]", c.history))
        }
      }
    }.withErrorMessage("[A]NonEmptyList[A]")

  /**
   * @group Disjunction
   */
  def decodeXor[A, B](leftKey: String, rightKey: String)(implicit
    da: Decode[A],
    db: Decode[B]
  ): Decode[Xor[A, B]] = instance { c =>
    val l = (c.downField(leftKey)).success
    val r = (c.downField(rightKey)).success

    (l, r) match {
      case (Some(hcursor), None) => da(hcursor).map(Xor.left(_))
      case (None, Some(hcursor)) => db(hcursor).map(Xor.right(_))
      case _ => Xor.left(DecodeFailure("[A, B]Xor[A, B]", c.history))
    }
  }

  /**
   * @group Disjunction
   */
  def decodeEither[A, B](leftKey: String, rightKey: String)(implicit
    da: Decode[A],
    db: Decode[B]
  ): Decode[Either[A, B]] =
    decodeXor[A, B](leftKey, rightKey).map(_.toEither).withErrorMessage("[A, B]Either[A, B]")

  /**
   * @group Disjunction
   */
  def decodeValidated[E, A](failureKey: String, successKey: String)(implicit
    de: Decode[E],
    da: Decode[A]
  ): Decode[Validated[E, A]] =
    decodeXor[E, A](
      failureKey,
      successKey
    ).map(_.toValidated).withErrorMessage("[E, A]Validated[E, A]")

  /**
   * @group Instances
   */
  implicit val monadDecode: Monad[Decode] = new Monad[Decode] {
    def pure[A](a: A): Decode[A] = instance(_ => Xor.right(a))
    def flatMap[A, B](fa: Decode[A])(f: A => Decode[B]): Decode[B] = fa.flatMap(f)
  }
}
