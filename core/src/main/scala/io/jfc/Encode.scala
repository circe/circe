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
   * Converts a value 
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
 * @author Travis Brown
 */
object Encode {
  /**
   * Return an instance for a given type.
   */
  def apply[A](implicit e: Encode[A]): Encode[A] = e

  implicit def fromCodec[A](implicit c: Codec[A]): Encode[A] = c.encoder

  def instance[A](f: A => Json): Encode[A] = new Encode[A] {
    def apply(a: A): Json = f(a)
  }

  def fromFoldable[F[_], A](implicit e: Encode[A], F: Foldable[F]): Encode[F[A]] =
    instance(fa =>
      Json.array(F.foldLeft(fa, Nil: List[Json])((list, a) => e(a) :: list).reverse: _*)
    )

  implicit def encodeTraversableOnce[A0, C[_]](implicit
    e: Encode[A0],
    is: collection.generic.IsTraversableOnce[C[A0]] { type A = A0 }
  ): Encode[C[A0]] =
    instance { list =>
    val items = collection.mutable.ArrayBuffer.empty[Json]

    is.conversion(list).foreach { a =>
      items += (e(a))
    }

    Json.fromValues(items)
  }

  implicit val encodeJson: Encode[Json] = instance(identity)
  implicit val encodeString: Encode[String] = instance(Json.string)

  implicit val encodeUnit: Encode[Unit] = instance(_ => Json.obj())

  implicit val encodeBoolean: Encode[Boolean] = instance(Json.bool)
  implicit val encodeChar: Encode[Char] = instance(a => Json.string(a.toString))

  implicit val encodeFloat: Encode[Float] = instance(a => JsonDouble(a.toDouble).asJsonOrNull)
  implicit val encodeDouble: Encode[Double] = instance(a => JsonDouble(a).asJsonOrNull)

  implicit val encodeByte: Encode[Byte] = instance(a => JsonLong(a.toLong).asJsonOrNull)
  implicit val encodeShort: Encode[Short] = instance(a => JsonLong(a.toLong).asJsonOrNull)
  implicit val encodeInt: Encode[Int] = instance(a => JsonLong(a.toLong).asJsonOrNull)
  implicit val encodeLong: Encode[Long] = instance(a => JsonLong(a).asJsonOrNull)

  implicit val encodeBigInt: Encode[BigInt] =
    instance(a => JsonBigDecimal(BigDecimal(a, java.math.MathContext.UNLIMITED)).asJsonOrNull)

  implicit val encodeBigDecimal: Encode[BigDecimal] = instance(a => JsonBigDecimal(a).asJsonOrNull)

  implicit def encodeOption[A](implicit e: Encode[A]): Encode[Option[A]] =
    instance(_.fold(Json.empty)(e(_)))


  implicit def encodeMapLike[M[K, +V] <: Map[K, V], V](implicit
    e: Encode[V]
  ): Encode[M[String, V]] = instance(m =>
    Json.fromFields(
      m.toList.map {
        case (k, v) => (k, e(v))
      }
    )
  )

  implicit def encodeNonEmptyList[A: Encode]: Encode[NonEmptyList[A]] =
    fromFoldable[NonEmptyList, A]


  def encodeXor[A, B](leftKey: String, rightKey: String)(implicit
    ea: Encode[A],
    eb: Encode[B]
  ): Encode[Xor[A, B]] = instance(
    _.fold(
      a => Json.obj(leftKey -> ea(a)),
      b => Json.obj(rightKey -> eb(b))
    )
  )

  def encodeEither[A, B](leftKey: String, rightKey: String)(implicit
    ea: Encode[A],
    eb: Encode[B]
  ): Encode[Either[A, B]] = encodeXor[A, B](leftKey, rightKey).contramap(Xor.fromEither)

  def encodeValidated[E, A](failureKey: String, successKey: String)(implicit
    ee: Encode[E],
    ea: Encode[A]
  ): Encode[Validated[E, A]] = encodeXor[E, A](failureKey, successKey).contramap(_.toXor)

  implicit val contravariantEncode: Contravariant[Encode] = new Contravariant[Encode] {
    def contramap[A, B](e: Encode[A])(f: B => A): Encode[B] = e.contramap(f)
  }
}
