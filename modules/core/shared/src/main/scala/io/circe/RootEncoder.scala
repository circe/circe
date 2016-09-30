package io.circe

/**
 * A subtype of `Encoder` that statically verifies that the instance encodes
 * either a JSON array or an object.
 *
 * @author Travis Brown
 */
trait RootEncoder[A] extends Encoder[A]

final object RootEncoder extends LowPriorityRootEncoders {
  /**
   * Return an instance for a given type.
   *
   * @group Utilities
   */
  final def apply[A](implicit instance: RootEncoder[A]): RootEncoder[A] = instance
}

@export.imports[ObjectEncoder] private[circe] trait LowPriorityRootEncoders
