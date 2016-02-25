package io.circe

/**
 * A type class that provides a conversion from a value of type `A` to a [[Json]] key.
 *
 */

trait KeyEncoder[A] { self =>
  /**
   * Converts a value to String
   */
  def toJsonKey(key: A): String

  /**
   * Constructs a KeyEncoder type class instance for type `B` from an instance for type `A`
   */
  final def contramap[B](f: B => A): KeyEncoder[B] =
    new KeyEncoder[B] {
      def toJsonKey(key: B): String =
        self.toJsonKey(f(key))
    }

}

object KeyEncoder {

  @inline def apply[A](implicit A: KeyEncoder[A]): KeyEncoder[A] = A

  def from[A](f: A => String): KeyEncoder[A] =
    new KeyEncoder[A] {
      def toJsonKey(key: A): String = f(key)
    }

}
