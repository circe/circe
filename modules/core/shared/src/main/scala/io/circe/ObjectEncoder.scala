package io.circe

import io.circe.ast.{ Json, JsonObject }
import io.circe.export.Exported

/**
 * A type class that provides a conversion from a value of type `A` to a
 * [[io.circe.ast.JsonObject]].
 *
 * @author Travis Brown
 */
trait ObjectEncoder[A] extends RootEncoder[A] { self =>
  final def apply(a: A): Json = Json.JObject(encodeObject(a))

  /**
   * Convert a value to a JSON object.
   */
  def encodeObject(a: A): JsonObject

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
}

private[circe] trait LowPriorityObjectEncoders {
  implicit final def importedObjectEncoder[A](implicit
    exported: Exported[ObjectEncoder[A]]
  ): ObjectEncoder[A] = exported.instance
}
