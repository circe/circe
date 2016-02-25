package io.circe

import cats.functor.Contravariant

/**
 * A type class that provides a conversion from a value of type `A` to a string.
 *
 * This type class will be used to create strings for JSON keys when encoding
 * `Map[A, ?]` instances as JSON.
 *
 * Note that if more than one value maps to the same string, the resuling JSON
 * object may have fewer fields than the original map.
 */
abstract class KeyEncoder[A] extends Serializable { self =>
  /**
   * Convert a key value to a string.
   */
  def apply(key: A): String

  /**
   * Construct an instance for type `B` from an instance for type `A`.
   */
  final def contramap[B](f: B => A): KeyEncoder[B] = new KeyEncoder[B] {
    def apply(key: B): String = self(f(key))
  }
}

final object KeyEncoder {
  @inline def apply[A](implicit A: KeyEncoder[A]): KeyEncoder[A] = A

  def instance[A](f: A => String): KeyEncoder[A] = new KeyEncoder[A] {
    def apply(key: A): String = f(key)
  }

  implicit val encodeKeyString: KeyEncoder[String] = instance(identity)
  implicit val encodeKeySymbol: KeyEncoder[Symbol] = instance(_.name)
  implicit val encodeKeyByte: KeyEncoder[Byte] = instance(_.toString)
  implicit val encodeKeyShort: KeyEncoder[Short] = instance(_.toString)
  implicit val encodeKeyInt: KeyEncoder[Int] = instance(_.toString)
  implicit val encodeKeyLong: KeyEncoder[Long] = instance(_.toString)

  implicit val contravariantKeyEncode: Contravariant[KeyEncoder] = new Contravariant[KeyEncoder] {
    final def contramap[A, B](e: KeyEncoder[A])(f: B => A): KeyEncoder[B] = e.contramap(f)
  }
}
