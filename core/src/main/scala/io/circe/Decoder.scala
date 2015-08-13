package io.circe

import scala.collection.generic.CanBuildFrom

trait Decoder[A] { self =>
  /**
   * Decode the given hcursor.
   */
  def apply(c: HCursor): Either[DecodingFailure, A]

  /**
   * Decode the given acursor.
   */
  def tryDecode(c: ACursor): Either[DecodingFailure, A] = c.either.fold(
    invalid =>
      Left(DecodingFailure("Attempt to decode value on failed cursor", invalid.history)),
    apply
  )

  /**
   * Decode the given [[Json]] value.
   */
  def decodeJson(j: Json): Either[DecodingFailure, A] = apply(j.cursor.hcursor)

  /**
   * Map a function over this [[Decoder]].
   */
  def map[B](f: A => B): Decoder[B] = new Decoder[B] {
    def apply(c: HCursor): Either[DecodingFailure, B] = self(c).right.map(f)
    override def tryDecode(c: ACursor): Either[DecodingFailure, B] = self.tryDecode(c).right.map(f)
  }

  /**
   * Monadically bind a function over this [[Decoder]].
   */
  def flatMap[B](f: A => Decoder[B]): Decoder[B] = new Decoder[B] {
    def apply(c: HCursor): Either[DecodingFailure, B] = {
      self(c).right.flatMap(a => f(a)(c))
    }

    override def tryDecode(c: ACursor): Either[DecodingFailure, B] = {
      self.tryDecode(c).right.flatMap(a => f(a).tryDecode(c))
    }
  }

  /**
   * Build a new instance with the specified error message.
   */
  def withErrorMessage(message: String): Decoder[A] = Decoder.instance(c =>
    apply(c).left.map(_.withMessage(message))
  )

  /**
   * Build a new instance that fails if the condition does not hold.
   */
  def validate(pred: HCursor => Boolean, message: => String): Decoder[A] = Decoder.instance(c =>
    if (pred(c)) apply(c) else Left(DecodingFailure(message, c.history))
  )

  /**
   * Combine two decoders.
   */
  def &&&[B](x: Decoder[B]): Decoder[(A, B)] = Decoder.instance(c =>
    for {
      a <- this(c).right
      b <- x(c).right
    } yield (a, b)
  )

  /**
   * Choose the first succeeding decoder.
   */
  def |||[AA >: A](d: => Decoder[AA]): Decoder[AA] = Decoder.instance[AA] { c =>
    val res = apply(c).right.map(a => (a: AA))
    res.fold(_ => d(c), _ => res)
  }

  /**
   * Run one or another decoder.
   */
  def split[B](d: Decoder[B]): Either[HCursor, HCursor] => Either[DecodingFailure, Either[A, B]] =
    _.fold(
      c => this(c).right.map(Left(_)),
      c => d(c).right.map(Right(_))
    )

  /**
   * Run two decoders.
   */
  def product[B](x: Decoder[B]): (HCursor, HCursor) => Either[DecodingFailure, (A, B)] = (a1, a2) =>
    for {
      a <- this(a1).right
      b <- x(a2).right
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
  def instance[A](f: HCursor => Either[DecodingFailure, A]): Decoder[A] = new Decoder[A] {
    def apply(c: HCursor): Either[DecodingFailure, A] = f(c)
  }

  /**
   * Construct an instance from a function that will reattempt on failure.
   *
   * @group Utilities
   */
  def withReattempt[A](f: ACursor => Either[DecodingFailure, A]): Decoder[A] = new Decoder[A] {
    def apply(c: HCursor): Either[DecodingFailure, A] = tryDecode(c.acursor)

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
  implicit val decodeHCursor: Decoder[HCursor] = instance(Right(_))

  /**
   * @group Decoding
   */
  implicit val decodeJson: Decoder[Json] = instance(c => Right(c.focus))

  /**
   * @group Decoding
   */
  implicit val decodeString: Decoder[String] = instance { c =>
    c.focus match {
      case JString(string) => Right(string)
      case _ => Left(DecodingFailure("String", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit val decodeUnit: Decoder[Unit] = instance { c =>
    c.focus match {
      case JNull => Right(())
      case JObject(obj) if obj.isEmpty => Right(())
      case JArray(arr) if arr.length == 0 => Right(())
      case _ => Left(DecodingFailure("String", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit val decodeBoolean: Decoder[Boolean] = instance { c =>
    c.focus match {
      case JBoolean(b) => Right(b)
      case _ => Left(DecodingFailure("Boolean", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit val decodeChar: Decoder[Char] = instance { c =>
    c.focus match {
      case JString(string) if string.length == 1 => Right(string.charAt(0))
      case _ => Left(DecodingFailure("Char", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit val decodeFloat: Decoder[Float] = instance { c =>
    c.focus match {
      case JNull => Right(Float.NaN)
      case JNumber(number) => Right(number.toDouble.toFloat)
      case _ => Left(DecodingFailure("Float", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit val decodeDouble: Decoder[Double] = instance { c =>
    c.focus match {
      case JNull => Right(Double.NaN)
      case JNumber(number) => Right(number.toDouble)
      case _ => Left(DecodingFailure("Float", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit val decodeByte: Decoder[Byte] = instance { c =>
    c.focus match {
      case JNumber(number) => Right(number.truncateToByte)
      case JString(string) => try {
        Right(string.toByte)
      } catch {
        case _: NumberFormatException => Left(DecodingFailure("Byte", c.history))
      }
      case _ => Left(DecodingFailure("Byte", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit val decodeShort: Decoder[Short] = instance { c =>
    c.focus match {
      case JNumber(number) => Right(number.truncateToShort)
      case JString(string) => try {
        Right(string.toShort)
      } catch {
        case _: NumberFormatException => Left(DecodingFailure("Short", c.history))
      }
      case _ => Left(DecodingFailure("Short", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit val decodeInt: Decoder[Int] = instance { c =>
    c.focus match {
      case JNumber(number) => Right(number.truncateToInt)
      case JString(string) => try {
        Right(string.toInt)
      } catch {
        case _: NumberFormatException => Left(DecodingFailure("Int", c.history))
      }
      case _ => Left(DecodingFailure("Int", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit val decodeLong: Decoder[Long] = instance { c =>
    c.focus match {
      case JNumber(number) => Right(number.truncateToLong)
      case JString(string) => try {
        Right(string.toLong)
      } catch {
        case _: NumberFormatException => Left(DecodingFailure("Long", c.history))
      }
      case _ => Left(DecodingFailure("Long", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit val decodeBigInt: Decoder[BigInt] = instance { c =>
    c.focus match {
      case JNumber(number) => Right(number.truncateToBigInt)
      case JString(string) => try {
        Right(BigInt(string))
      } catch {
        case _: NumberFormatException => Left(DecodingFailure("BigInt", c.history))
      }
      case _ => Left(DecodingFailure("BigInt", c.history))
    }
  }

  /**
   * @group Decoding
   */
  implicit val decodeBigDecimal: Decoder[BigDecimal] = instance { c =>
    c.focus match {
      case JNumber(number) => Right(number.toBigDecimal)
      case JString(string) => try {
        Right(BigDecimal(string))
      } catch {
        case _: NumberFormatException => Left(DecodingFailure("BigDecimal", c.history))
      }
      case _ => Left(DecodingFailure("BigDecimal", c.history))
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
        Right(cbf.apply.result)
      else
        Left(DecodingFailure("CanBuildFrom for A", c.history))
    )(
      _.traverseDecode(cbf.apply)(
        _.right,
        (acc, hcursor) => hcursor.as[A].right.map(acc += _)
      ).right.map(_.result)
    )
  }

  /**
   * @group Decoding
   */
  implicit def decodeOption[A](implicit d: Decoder[A]): Decoder[Option[A]] =
    withReattempt { a =>
      a.success.fold[Either[DecodingFailure, Option[A]]](Right(None)) { valid =>
        if (valid.focus.isNull) Right(None) else d(valid).fold[Either[DecodingFailure, Option[A]]](
          df =>
            df.history.headOption.fold[Either[DecodingFailure, Option[A]]](
              Right(None)
            )(_ => Left(df)),
          a => Right(Some(a))
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
    c.fields.fold[Either[DecodingFailure, M[String, V]]](
      Left(DecodingFailure("[V]Map[String, V]", c.history))
    ) { s =>
      @scala.annotation.tailrec
      def spin(
        x: List[String],
        acc: Either[DecodingFailure, Vector[(String, V)]]
      ): Either[DecodingFailure, M[String, V]] =
        x match {
          case Nil =>
            acc.right.map { fields =>
              (cbf() ++= fields).result()
            }
          case h :: t =>
            val acc0 = for {
              m <- acc.right
              v <- c.get(h)(d).right
            } yield m :+ (h -> v)

            if (acc0.isLeft) spin(Nil, acc0)
            else spin(t, acc0)
        }
      spin(s, Right(Vector.empty))
    }
  }

  /**
   * @group Decoding
   */
  implicit def decodeSet[A: Decoder]: Decoder[Set[A]] =
    decodeCanBuildFrom[A, List].map(_.toSet).withErrorMessage("[A]Set[A]")

  /**
   * @group Disjunction
   */
  def decodeEither[A, B](leftKey: String, rightKey: String)(implicit
    da: Decoder[A],
    db: Decoder[B]
  ): Decoder[Either[A, B]] = instance { c =>
    val l = (c.downField(leftKey)).success
    val r = (c.downField(rightKey)).success

    (l, r) match {
      case (Some(hcursor), None) => da(hcursor).right.map(Left(_))
      case (None, Some(hcursor)) => db(hcursor).right.map(Right(_))
      case _ => Left(DecodingFailure("[A, B]Either[A, B]", c.history))
    }
  }
}
