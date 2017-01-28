package io.circe

import cats.functor.Contravariant

/**
 * A type class that provides a conversion from a value of type `A` to a JSON
 * array.
 *
 * @author Travis Brown
 */
trait ArrayEncoder[A] extends RootEncoder[A] { self =>
  final def apply(a: A): Json = Json.JArray(encodeArray(a))

  /**
   * Convert a value to a JSON array.
   */
  def encodeArray(a: A): Vector[Json]

  /**
    * Create a new [[ArrayEncoder]] by applying a function to a value of type `B` before encoding as
    * an `A`.
    */
  final def contramapArray[B](f: B => A): ArrayEncoder[B] = new ArrayEncoder[B] {
    final def encodeArray(a: B) = self.encodeArray(f(a))
  }

  /**
   * Create a new [[ArrayEncoder]] by applying a function to the output of this
   * one.
   */
  final def mapJsonArray(f: Vector[Json] => Vector[Json]): ArrayEncoder[A] = new ArrayEncoder[A] {
    final def encodeArray(a: A): Vector[Json] = f(self.encodeArray(a))
  }
}

final object ArrayEncoder {
  /**
   * Return an instance for a given type.
   *
   * @group Utilities
   */
  final def apply[A](implicit instance: ArrayEncoder[A]): ArrayEncoder[A] = instance

  /**
   * Construct an instance from a function.
   *
   * @group Utilities
   */
  final def instance[A](f: A => Vector[Json]): ArrayEncoder[A] = new ArrayEncoder[A] {
    final def encodeArray(a: A): Vector[Json] = f(a)
  }

  /**
    * @group Instances
    */
  implicit final val arrayEncoderContravariant: Contravariant[ArrayEncoder] = new Contravariant[ArrayEncoder] {
    final def contramap[A, B](e: ArrayEncoder[A])(f: B => A): ArrayEncoder[B] = e.contramapArray(f)
  }
}
