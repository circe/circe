package io.circe

/**
 * A type class that provides a conversion from a value of type `A` to a [[JsonObject]].
 *
 * @author Travis Brown
 */
trait ObjectEncoder[A] extends Encoder[A] { self =>
  def apply(a: A): Json = Json.JObject(encodeObject(a))

  /**
   * Convert a value to a JSON object.
   */
  def encodeObject(a: A): JsonObject
}

object ObjectEncoder extends LowPriorityObjectEncoders {
  /**
   * Return an instance for a given type.
   *
   * @group Utilities
   */
  def apply[A](implicit e: ObjectEncoder[A]): ObjectEncoder[A] = e

  /**
   * Construct an instance from a function.
   *
   * @group Utilities
   */
  def instance[A](f: A => JsonObject): ObjectEncoder[A] = new ObjectEncoder[A] {
    def encodeObject(a: A): JsonObject = f(a)
  }
}

@export.exported[ObjectEncoder] trait LowPriorityObjectEncoders
