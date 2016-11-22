package io.circe

import io.circe.ast.Json

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
  def encodeArray(a: A): List[Json]

  /**
   * Create a new [[ArrayEncoder]] by applying a function to the output of this
   * one.
   */
  final def mapJsonArray(f: List[Json] => List[Json]): ArrayEncoder[A] = new ArrayEncoder[A] {
    final def encodeArray(a: A): List[Json] = f(self.encodeArray(a))
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
  final def instance[A](f: A => List[Json]): ArrayEncoder[A] = new ArrayEncoder[A] {
    final def encodeArray(a: A): List[Json] = f(a)
  }
}
