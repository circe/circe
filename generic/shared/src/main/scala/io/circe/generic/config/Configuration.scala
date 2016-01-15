package io.circe.generic.config

import io.circe.generic.DerivationMacros
import scala.language.experimental.macros

/**
 * A runtime representation of a set of configuration options.
 *
 * Users should not instantiate this type class. It is provided only as an
 * optimization to simplify generic derivation.
 */
final case class Configuration[C] private[generic](
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
