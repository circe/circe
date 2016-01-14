package io.circe.generic.config

import io.circe.generic.DerivationMacros
import scala.language.experimental.macros
import shapeless.Witness

sealed trait KeyTransformation {
  def apply(s: String): String
}
trait KeyIdentity extends KeyTransformation {
  final def apply(s: String): String = s
}
final object KeyIdentity extends KeyIdentity

trait SnakeCaseKeys extends KeyTransformation {
  final def apply(s: String): String = s.replaceAll(
    "([A-Z]+)([A-Z][a-z])",
    "$1_$2"
  ).replaceAll("([a-z\\d])([A-Z])", "$1_$2").toLowerCase
}
final object SnakeCaseKeys extends SnakeCaseKeys

sealed trait Discriminator

trait ObjectWrapper extends Discriminator
final object ObjectWrapper extends ObjectWrapper

class DiscriminatorField[K <: String](implicit w: Witness.Aux[K]) extends Discriminator {
  final val key: String = w.value
}

object DiscriminatorKey {
  def unapply(discriminator: Discriminator): Option[String] = discriminator match {
    case field: DiscriminatorField[_] => Some(field.key)
    case _ => None
  }
}

class TypeField extends DiscriminatorField[Witness.`"type"`.T]
final object TypeField extends TypeField

sealed trait CaseObjectEncoding

trait CaseObjectObject extends CaseObjectEncoding
final object CaseObjectObject extends CaseObjectObject

trait CaseObjectString extends CaseObjectEncoding
final object CaseObjectString extends CaseObjectString

sealed trait DefaultValues

trait NoDefaultValues extends DefaultValues
final object NoDefaultValues extends NoDefaultValues

trait UseDefaultValues extends DefaultValues
final object UseDefaultValues extends UseDefaultValues

case class Configuration[C](
  keyTransformation: KeyTransformation,
  discriminator: Discriminator,
  caseObjectEncoding: CaseObjectEncoding,
  defaultValues: DefaultValues
)

object Configuration {
  final val default: Configuration[KeyIdentity with ObjectWrapper with CaseObjectObject with NoDefaultValues] =
    Configuration(KeyIdentity, ObjectWrapper, CaseObjectObject, NoDefaultValues)

  implicit def materializeConfiguration[C]: Configuration[C] =
    macro DerivationMacros.materializeConfiguration[C]
}
