package io.circe

import cats.data._
import cats.functor.Contravariant
import cats.Foldable
import java.util.UUID
import scala.collection.GenSeq
import scala.collection.generic.IsTraversableOnce
import scala.collection.mutable.ArrayBuffer

/**
 * A type class that provides a conversion from a value of type `A` to a [[Json]] value.
 *
 * @author Travis Brown
 */
trait Encoder[A] extends Serializable {
  /**
   * Converts a value to JSON.
   */
  def apply(a: A): Json

  /**
   * Creates a new instance by applying a function to a value of type `B` before encoding as an `A`.
   */
  final def contramap[B](f: B => A): Encoder[B] = Encoder.instance(b => apply(f(b)))
}

/**
 * Utilities and instances for [[Encoder]].
 *
 * @groupname Utilities Miscellaneous utilities
 * @groupprio Utilities 0
 *
 * @groupname Encoding Encoder instances
 * @groupprio Encoding 1
 *
 * @groupname Disjunction Disjunction instances
 * @groupdesc Disjunction Instance creation methods for disjunction-like types.
 * Note that these are implicit, since they require non-obvious decisions about
 * the names of the discriminators. If you want instances for these types you
 * can include the following import in your program:
 * {{{
 *   import io.circe.disjunctionCodecs._
 * }}}
 * @groupprio Disjunction 2
 *
 * @groupname Instances Type class instances
 * @groupprio Instances 3
 *
 * @groupname Tuple Tuple instances
 * @groupprio Tuple 4
 *
 * @author Travis Brown
 */
object Encoder extends TupleEncoders with LowPriorityEncoders {
  /**
   * Return an instance for a given type `A`.
   *
   * @group Utilities
   */
  final def apply[A](implicit e: Encoder[A]): Encoder[A] = e

  /**
   * Construct an instance from a function.
   *
   * @group Utilities
   */
  final def instance[A](f: A => Json): Encoder[A] = new Encoder[A] {
    final def apply(a: A): Json = f(a)
  }

  /**
   * Construct an instance for a given type with a [[cats.Foldable]] instance.
   *
   * @group Utilities
   */
  final def fromFoldable[F[_], A](implicit e: Encoder[A], F: Foldable[F]): Encoder[F[A]] =
    instance(fa =>
      Json.fromValues(F.foldLeft(fa, List.empty[Json])((list, a) => e(a) :: list).reverse)
    )

  /**
   * @group Encoding
   */
  implicit final def encodeTraversableOnce[A0, C[_]](implicit
    e: Encoder[A0],
    is: IsTraversableOnce[C[A0]] { type A = A0 }
  ): Encoder[C[A0]] =
    instance { list =>
    val items = ArrayBuffer.empty[Json]

    is.conversion(list).foreach { a =>
      items += e(a)
    }

    Json.fromValues(items)
  }

  /**
   * @group Encoding
   */
  implicit final val encodeJson: Encoder[Json] = instance(identity)

  /**
   * @group Encoding
   */
  implicit final val encodeJsonObject: Encoder[JsonObject] = instance(Json.fromJsonObject)

  /**
   * @group Encoding
   */
  implicit final val encodeJsonNumber: Encoder[JsonNumber] = instance(Json.fromJsonNumber)

  /**
   * @group Encoding
   */
  implicit final val encodeString: Encoder[String] = instance(Json.string)

  /**
   * @group Encoding
   */
  implicit final val encodeUnit: Encoder[Unit] = instance(_ => Json.obj())

  /**
   * @group Encoding
   */
  implicit final val encodeBoolean: Encoder[Boolean] = instance(Json.bool)

  /**
   * @group Encoding
   */
  implicit final val encodeChar: Encoder[Char] = instance(a => Json.string(a.toString))

  /**
   * @group Encoding
   */
  implicit final val encodeFloat: Encoder[Float] =
    instance(a => JsonDouble(a.toDouble).asJsonOrNull)

  /**
   * @group Encoding
   */
  implicit final val encodeDouble: Encoder[Double] = instance(a => JsonDouble(a).asJsonOrNull)

  /**
   * @group Encoding
   */
  implicit final val encodeByte: Encoder[Byte] = instance(a => Json.JNumber(JsonLong(a.toLong)))

  /**
   * @group Encoding
   */
  implicit final val encodeShort: Encoder[Short] = instance(a => Json.JNumber(JsonLong(a.toLong)))

  /**
   * @group Encoding
   */
  implicit final val encodeInt: Encoder[Int] = instance(a => Json.JNumber(JsonLong(a.toLong)))

  /**
   * @group Encoding
   */
  implicit final val encodeLong: Encoder[Long] = instance(a => Json.JNumber(JsonLong(a)))

  /**
   * @group Encoding
   */
  implicit final val encodeBigInt: Encoder[BigInt] =
    instance(a => JsonBigDecimal(BigDecimal(a, java.math.MathContext.UNLIMITED)).asJsonOrNull)

  /**
   * @group Encoding
   */
  implicit final val encodeBigDecimal: Encoder[BigDecimal] =
    instance(a => JsonBigDecimal(a).asJsonOrNull)

  /**
   * @group Encoding
   */
  implicit final val encodeUUID: Encoder[UUID] = instance(a => Json.string(a.toString))

  /**
   * @group Encoding
   */
  implicit final def encodeOption[A](implicit e: Encoder[A]): Encoder[Option[A]] =
    instance(_.fold(Json.empty)(e(_)))

  /**
   * @group Encoding
   */
  implicit final def encodeOneAnd[A0, C[_]](
    implicit ea: Encoder[A0],
    is: IsTraversableOnce[C[A0]] { type A = A0 }
  ): Encoder[OneAnd[C, A0]] = encodeTraversableOnce[A0, GenSeq].contramap[OneAnd[C, A0]] {
    oneAnd => oneAnd.head +: is.conversion(oneAnd.tail).toSeq
  }

  /**
   * @group Encoding
   */
  implicit final def encodeMapLike[M[K, +V] <: Map[K, V], V](implicit
    e: Encoder[V]
  ): ObjectEncoder[M[String, V]] = ObjectEncoder.instance(m =>
    JsonObject.fromMap(
      m.map {
        case (k, v) => (k, e(v))
      }
    )
  )

  /**
   * @group Disjunction
   */
  final def encodeXor[A, B](leftKey: String, rightKey: String)(implicit
    ea: Encoder[A],
    eb: Encoder[B]
  ): ObjectEncoder[Xor[A, B]] = ObjectEncoder.instance(
    _.fold(
      a => JsonObject.singleton(leftKey, ea(a)),
      b => JsonObject.singleton(rightKey, eb(b))
    )
  )

  /**
   * @group Disjunction
   */
  final def encodeEither[A, B](leftKey: String, rightKey: String)(implicit
    ea: Encoder[A],
    eb: Encoder[B]
  ): ObjectEncoder[Either[A, B]] = ObjectEncoder.instance(
    _.fold(
      a => JsonObject.singleton(leftKey, ea(a)),
      b => JsonObject.singleton(rightKey, eb(b))
    )
  )

  /**
   * @group Disjunction
   */
  final def encodeValidated[E, A](failureKey: String, successKey: String)(implicit
    ee: Encoder[E],
    ea: Encoder[A]
  ): ObjectEncoder[Validated[E, A]] = ObjectEncoder.instance(
    _.fold(
      e => JsonObject.singleton(failureKey, ee(e)),
      a => JsonObject.singleton(successKey, ea(a))
    )
  )

  /**
   * @group EncodeJson
   */
  final def mapLikeEncodeJson[M[K, +V] <: Map[K, V], K, V](implicit
    ke: KeyEncoder[K],
    ev: Encoder[V]
  ): Encoder[M[K, V]] = ObjectEncoder.instance(m =>
    JsonObject.fromMap(
      m.map {
        case (k, v) => (ke.toJsonKey(k), ev(v))
      }
    )
  )

  /**
   * @group Instances
   */
  implicit final val contravariantEncode: Contravariant[Encoder] = new Contravariant[Encoder] {
    final def contramap[A, B](e: Encoder[A])(f: B => A): Encoder[B] = e.contramap(f)
  }
}

@export.imports[Encoder] private[circe] trait LowPriorityEncoders
