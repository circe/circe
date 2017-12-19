package io.circe

import cats.functor.Contravariant
import io.circe.export.Exported

/**
 * A type class that provides a conversion from a value of type `A` to a
 * [[JsonObject]].
 *
 * @author Travis Brown
 */
trait ObjectEncoder[A] extends RootEncoder[A] { self =>
  final def apply(a: A): Json = Json.fromJsonObject(encodeObject(a))

  /**
   * Convert a value to a JSON object.
   */
  def encodeObject(a: A): JsonObject

  /**
    * Create a new [[ObjectEncoder]] by applying a function to a value of type `B` before encoding as an
    * `A`.
    */
  final def contramapObject[B](f: B => A): ObjectEncoder[B] = new ObjectEncoder[B] {
    final def encodeObject(a: B) = self.encodeObject(f(a))
  }

  /**
   * Create a new [[ObjectEncoder]] by applying a function to the output of this
   * one.
   */
  final def mapJsonObject(f: JsonObject => JsonObject): ObjectEncoder[A] = new ObjectEncoder[A] {
    final def encodeObject(a: A): JsonObject = f(self.encodeObject(a))
  }
}

final object ObjectEncoder extends LowPriorityObjectEncoders {
  /**
   * Return an instance for a given type.
   *
   * @group Utilities
   */
  final def apply[A](implicit instance: ObjectEncoder[A]): ObjectEncoder[A] = instance

  /**
   * Construct an instance from a function.
   *
   * @group Utilities
   */
  final def instance[A](f: A => JsonObject): ObjectEncoder[A] = new ObjectEncoder[A] {
    final def encodeObject(a: A): JsonObject = f(a)
  }

  /**
    * @group Instances
    */
  implicit final val objectEncoderContravariant: Contravariant[ObjectEncoder] = new Contravariant[ObjectEncoder] {
    final def contramap[A, B](e: ObjectEncoder[A])(f: B => A): ObjectEncoder[B] = e.contramapObject(f)
  }
}

private[circe] trait LowPriorityObjectEncoders {
  implicit final def importedObjectEncoder[A](implicit
    exported: Exported[ObjectEncoder[A]]
  ): ObjectEncoder[A] = exported.instance
}
