package io.circe.generic.config

import shapeless.Witness

/**
 * Determines how case classes and objects in sealed trait hierarchies should be
 * differentiated.
 */
sealed trait Discriminator

/**
 * Wraps the JSON representation of the case class or object in a JSON object.
 *
 * The outer object will contain a single field with the name of the case class
 * or object as the key. This is the "safest" method, since it's not subject to
 * collisions or the need to fall back to other approaches if the representation
 * of the type isn't a JSON object.
 */
trait ObjectWrapper extends Discriminator
final object ObjectWrapper extends ObjectWrapper

/**
 * Adds a type field to the JSON object representing the case class or object.
 *
 * If there is an existing field with the discriminating field's key, it will be
 * replaced, and if the representation of the type isn't a JSON object, the
 * [[ObjectWrapper]] approach will be used.
 *
 * @tparam K A type-level string that will be used as the field's key
 */
class DiscriminatorField[K <: String](implicit w: Witness.Aux[K]) extends Discriminator {
  final val key: String = w.value
}

object DiscriminatorKey {
  def unapply(discriminator: Discriminator): Option[String] = discriminator match {
    case field: DiscriminatorField[_] => Some(field.key)
    case _ => None
  }
}

/**
 * A [[DiscriminatorField]] instance with "type" as the key (provided for
 * convenience, since this is the most common case).
 */
class TypeField extends DiscriminatorField[Witness.`"type"`.T]
final object TypeField extends TypeField
