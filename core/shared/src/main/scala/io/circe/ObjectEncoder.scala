package io.circe

/**
 * A type class that provides a conversion from a value of type `A` to a [[JsonObject]].
 *
 * @author Travis Brown
 */
trait ObjectEncoder[A] extends Encoder[A] { self =>
  final def apply(a: A): Json = Json.JObject(encodeObject(a))

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
  final def apply[A](implicit e: ObjectEncoder[A]): ObjectEncoder[A] = e

  /**
   * Construct an instance from a function.
   *
   * @group Utilities
   */
  final def instance[A](f: A => JsonObject): ObjectEncoder[A] = new ObjectEncoder[A] {
    final def encodeObject(a: A): JsonObject = f(a)
  }
}

@export.imports[ObjectEncoder]
private[circe] trait LowPriorityObjectEncoders

trait ConfiguredObjectEncoder[C, A] extends ObjectEncoder[A] with ConfiguredEncoder[C, A]

@export.imports[ConfiguredObjectEncoder]
object ConfiguredObjectEncoder
