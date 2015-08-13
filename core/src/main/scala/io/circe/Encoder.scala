package io.circe

import scala.collection.generic.IsTraversableOnce
import scala.collection.mutable.ArrayBuffer

/**
 * A type class that provides a conversion from a value of type `A` to a [[Json]] value.
 *
 * @author Travis Brown
 */
trait Encoder[A] {
  /**
   * Converts a value to JSON.
   */
  def apply(a: A): Json

  /**
   * Creates a new instance by applying a function to a value of type `B` before encoding as an `A`.
   */
  def contramap[B](f: B => A): Encoder[B] = Encoder.instance(b => apply(f(b)))
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
 * @author Travis Brown
 */
object Encoder {
  /**
   * A wrapper that supports proper prioritization of derived instances.
   *
   * @group Utilities
   */
  class Secondary[A](val value: Encoder[A]) extends AnyVal

  /**
   * Return an instance for a given type `A`.
   *
   * @group Utilities
   */
  def apply[A](implicit e: Encoder[A]): Encoder[A] = e

  /**
   * Construct an instance from a function.
   *
   * @group Utilities
   */
  def instance[A](f: A => Json): Encoder[A] = new Encoder[A] {
    def apply(a: A): Json = f(a)
  }

  /**
   * Unwrap a [[Secondary]] wrapper.
   *
   * @group Encoding
   */
  implicit def fromSecondaryEncoder[A](implicit e: Secondary[A]): Encoder[A] = e.value

  /**
   * @group Encoding
   */
  implicit def encodeTraversableOnce[A0, C[_]](implicit
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
  implicit val encodeJson: Encoder[Json] = instance(identity)

  /**
   * @group Encoding
   */
  implicit val encodeString: Encoder[String] = instance(Json.string)

  /**
   * @group Encoding
   */
  implicit val encodeUnit: Encoder[Unit] = instance(_ => Json.obj())

  /**
   * @group Encoding
   */
  implicit val encodeBoolean: Encoder[Boolean] = instance(Json.bool)

  /**
   * @group Encoding
   */
  implicit val encodeChar: Encoder[Char] = instance(a => Json.string(a.toString))

  /**
   * @group Encoding
   */
  implicit val encodeFloat: Encoder[Float] = instance(a => JsonDouble(a.toDouble).asJsonOrNull)

  /**
   * @group Encoding
   */
  implicit val encodeDouble: Encoder[Double] = instance(a => JsonDouble(a).asJsonOrNull)

  /**
   * @group Encoding
   */
  implicit val encodeByte: Encoder[Byte] = instance(a => Json.JNumber(JsonLong(a.toLong)))

  /**
   * @group Encoding
   */
  implicit val encodeShort: Encoder[Short] = instance(a => Json.JNumber(JsonLong(a.toLong)))

  /**
   * @group Encoding
   */
  implicit val encodeInt: Encoder[Int] = instance(a => Json.JNumber(JsonLong(a.toLong)))

  /**
   * @group Encoding
   */
  implicit val encodeLong: Encoder[Long] = instance(a => Json.JNumber(JsonLong(a)))

  /**
   * @group Encoding
   */
  implicit val encodeBigInt: Encoder[BigInt] =
    instance(a => JsonBigDecimal(BigDecimal(a, java.math.MathContext.UNLIMITED)).asJsonOrNull)

  /**
   * @group Encoding
   */
  implicit val encodeBigDecimal: Encoder[BigDecimal] = instance(a => JsonBigDecimal(a).asJsonOrNull)

  /**
   * @group Encoding
   */
  implicit def encodeOption[A](implicit e: Encoder[A]): Encoder[Option[A]] =
    instance(_.fold(Json.empty)(e(_)))

  /**
   * @group Encoding
   */
  implicit def encodeMapLike[M[K, +V] <: Map[K, V], V](implicit
    e: Encoder[V]
  ): ObjectEncoder[M[String, V]] = ObjectEncoder.instance(m =>
    JsonObject.fromIndexedSeq(
      m.toVector.map {
        case (k, v) => (k, e(v))
      }
    )
  )

  /**
   * @group Disjunction
   */
  def encodeEither[A, B](leftKey: String, rightKey: String)(implicit
    ea: Encoder[A],
    eb: Encoder[B]
  ): ObjectEncoder[Either[A, B]] = ObjectEncoder.instance(
    _.fold(
      a => JsonObject.singleton(leftKey, ea(a)),
      b => JsonObject.singleton(rightKey, eb(b))
    )
  )
}
