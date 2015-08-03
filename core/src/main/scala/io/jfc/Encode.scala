package io.jfc

import cats.Foldable
import cats.data.{ NonEmptyList, Validated, Xor }
import cats.functor.Contravariant
import cats.std.list._

/**
 * A type class that provides a conversion from a value of type `A` to a [[Json]].
 *
 * @author Travis Brown
 */
trait Encode[A] {
  /**
   * Converts a value to JSON.
   */
  def apply(a: A): Json

  /**
   * Creates a new instance by applying a function before encoding.
   */
  def contramap[B](f: B => A): Encode[B] = Encode.instance(b => apply(f(b)))
}

/**
 * Utilities and instances for [[Encode]].
 *
 * @groupname Utilities Miscellaneous utilities
 * @groupprio Utilities 0
 *
 * @groupname Encode Encode instances
 * @groupprio Encode 2
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
object Encode {
  /**
   * Return an instance for a given type.
   *
   * @group Utilities
   */
  def apply[A](implicit e: Encode[A]): Encode[A] = e

  /**
   * Construct an instance from a function.
   *
   * @group Utilities
   */
  def instance[A](f: A => Json): Encode[A] = new Encode[A] {
    def apply(a: A): Json = f(a)
  }

  /**
   * Construct an instance for a given type with a [[cats.Foldable]] instance.
   *
   * @group Utilities
   */
  def fromFoldable[F[_], A](implicit e: Encode[A], F: Foldable[F]): Encode[F[A]] =
    instance(fa =>
      Json.array(F.foldLeft(fa, Nil: List[Json])((list, a) => e(a) :: list).reverse: _*)
    )

  /**
   * @group Encode
   */
  implicit def fromCodec[A](implicit c: Codec[A]): Encode[A] = c.encoder

  /**
   * @group Encode
   */
  implicit def encodeTraversableOnce[A0, C[_]](implicit
    e: Encode[A0],
    is: collection.generic.IsTraversableOnce[C[A0]] { type A = A0 }
  ): Encode[C[A0]] =
    instance { list =>
    val items = collection.mutable.ArrayBuffer.empty[Json]

    is.conversion(list).foreach { a =>
      items += e(a)
    }

    Json.fromValues(items)
  }

  /**
   * @group Encode
   */
  implicit val encodeJson: Encode[Json] = instance(identity)

  /**
   * @group Encode
   */
  implicit val encodeString: Encode[String] = instance(Json.string)

  /**
   * @group Encode
   */
  implicit val encodeUnit: Encode[Unit] = instance(_ => Json.obj())

  /**
   * @group Encode
   */
  implicit val encodeBoolean: Encode[Boolean] = instance(Json.bool)

  /**
   * @group Encode
   */
  implicit val encodeChar: Encode[Char] = instance(a => Json.string(a.toString))

  /**
   * @group Encode
   */
  implicit val encodeFloat: Encode[Float] = instance(a => JsonDouble(a.toDouble).asJsonOrNull)

  /**
   * @group Encode
   */
  implicit val encodeDouble: Encode[Double] = instance(a => JsonDouble(a).asJsonOrNull)

  /**
   * @group Encode
   */
  implicit val encodeByte: Encode[Byte] = instance(a => JsonLong(a.toLong).asJsonOrNull)

  /**
   * @group Encode
   */
  implicit val encodeShort: Encode[Short] = instance(a => JsonLong(a.toLong).asJsonOrNull)

  /**
   * @group Encode
   */
  implicit val encodeInt: Encode[Int] = instance(a => JsonLong(a.toLong).asJsonOrNull)

  /**
   * @group Encode
   */
  implicit val encodeLong: Encode[Long] = instance(a => JsonLong(a).asJsonOrNull)

  /**
   * @group Encode
   */
  implicit val encodeBigInt: Encode[BigInt] =
    instance(a => JsonBigDecimal(BigDecimal(a, java.math.MathContext.UNLIMITED)).asJsonOrNull)

  /**
   * @group Encode
   */
  implicit val encodeBigDecimal: Encode[BigDecimal] = instance(a => JsonBigDecimal(a).asJsonOrNull)

  /**
   * @group Encode
   */
  implicit def encodeOption[A](implicit e: Encode[A]): Encode[Option[A]] =
    instance(_.fold(Json.empty)(e(_)))

  /**
   * @group Encode
   */
  implicit def encodeMapLike[M[K, +V] <: Map[K, V], V](implicit
    e: Encode[V]
  ): Encode[M[String, V]] = instance(m =>
    Json.fromFields(
      m.toList.map {
        case (k, v) => (k, e(v))
      }
    )
  )

  /**
   * @group Encode
   */
  implicit def encodeNonEmptyList[A: Encode]: Encode[NonEmptyList[A]] =
    fromFoldable[NonEmptyList, A]

  /**
   * @group Disjunction
   */
  def encodeXor[A, B](leftKey: String, rightKey: String)(implicit
    ea: Encode[A],
    eb: Encode[B]
  ): Encode[Xor[A, B]] = instance(
    _.fold(
      a => Json.obj(leftKey -> ea(a)),
      b => Json.obj(rightKey -> eb(b))
    )
  )

  /**
   * @group Disjunction
   */
  def encodeEither[A, B](leftKey: String, rightKey: String)(implicit
    ea: Encode[A],
    eb: Encode[B]
  ): Encode[Either[A, B]] = encodeXor[A, B](leftKey, rightKey).contramap(Xor.fromEither)

  /**
   * @group Disjunction
   */
  def encodeValidated[E, A](failureKey: String, successKey: String)(implicit
    ee: Encode[E],
    ea: Encode[A]
  ): Encode[Validated[E, A]] = encodeXor[E, A](failureKey, successKey).contramap(_.toXor)

  /**
   * @group Instances
   */
  implicit val contravariantEncode: Contravariant[Encode] = new Contravariant[Encode] {
    def contramap[A, B](e: Encode[A])(f: B => A): Encode[B] = e.contramap(f)
  }
}
