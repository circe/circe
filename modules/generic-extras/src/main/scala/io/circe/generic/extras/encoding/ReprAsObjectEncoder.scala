package io.circe.generic.extras.encoding

import io.circe.{ Encoder, Json, JsonObject }
import io.circe.generic.extras.ConfigurableDeriver
import scala.language.experimental.macros

/**
 * An encoder for a generic representation of a case class or ADT.
 *
 * Note that users typically will not work with instances of this class.
 */
trait ReprAsObjectEncoder[A] extends Encoder.AsObject[A] {
  def configuredEncodeObject(a: A)(
    transformMemberNames: String => String,
    transformDiscriminator: String => String,
    discriminator: Option[String]
  ): JsonObject

  final protected[this] def addDiscriminator[B](
    encode: Encoder[B],
    value: B,
    name: String,
    discriminator: Option[String]
  ): JsonObject = discriminator match {
    case None => JsonObject.singleton(name, encode(value))
    case Some(disc) =>
      encode match {
        case oe: Encoder.AsObject[B] @unchecked => oe.encodeObject(value).add(disc, Json.fromString(name))
        case _                                  => JsonObject.singleton(name, encode(value))
      }
  }

  final def encodeObject(a: A): JsonObject = configuredEncodeObject(a)(Predef.identity, Predef.identity, None)
}

object ReprAsObjectEncoder {
  implicit def deriveReprAsObjectEncoder[R]: ReprAsObjectEncoder[R] = macro ConfigurableDeriver.deriveEncoder[R]
}
