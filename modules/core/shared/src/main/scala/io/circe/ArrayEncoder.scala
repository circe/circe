package io.circe

import cats.Contravariant
import io.circe.export.Exported

/**
 * A type class that provides a conversion from a value of type `A` to a JSON
 * array.
 *
 * @author Travis Brown
 */
trait ArrayEncoder[A] extends RootEncoder[A] { self =>
  final def apply(a: A): Json = Json.fromValues(encodeArray(a))

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

/**
 * Utilities and instances for [[ArrayEncoder]].
 *
 * @groupname Utilities Defining encoders
 * @groupprio Utilities 1
 *
 * @groupname Instances Type class instances
 * @groupprio Instances 2
 *
 * @groupname Prioritization Instance prioritization
 * @groupprio Prioritization 3
 *
 * @author Travis Brown
 */
final object ArrayEncoder extends LowPriorityArrayEncoders {

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

private[circe] trait LowPriorityArrayEncoders {

  /**
   * @group Prioritization
   */
  implicit final def importedArrayEncoder[A](implicit exported: Exported[ArrayEncoder[A]]): ArrayEncoder[A] =
    exported.instance
}
