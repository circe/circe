package io.circe

import io.circe.export.Exported

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

private[circe] trait LowPriorityRootEncoders {
  implicit final def importedRootEncoder[A](implicit exported: Exported[ObjectEncoder[A]]): RootEncoder[A] =
    exported.instance
}
