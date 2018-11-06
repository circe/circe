package io.circe.generic.extras

/**
 * Configuration allowing customisation of the JSON produced when encoding, or expected when decoding. Can be used
 * with the [[ConfiguredJsonCodec]] annotation to allow customisation of the semi-automatic derivation.
 *
 * @param transformMemberNames Transforms the names of any case class members in the JSON allowing, for example,
 *                             formatting or case changes.
 * @param useDefaults Whether to allow default values as specified for any case-class members.
 * @param discriminator Optional key name that, when given, will be used to store the name of the constructor of an ADT
 *                      in a nested field with this name. If not given, the name is instead stored as a key under which
 *                      the contents of the ADT are stored as an object.
 * @param transformConstructorNames Transforms the value of any constructor names in the JSON allowing, for example,
 *                                  formatting or case changes.
 */
final case class Configuration(
  transformMemberNames: String => String,
  transformConstructorNames: String => String,
  useDefaults: Boolean,
  discriminator: Option[String]
) {
  def withSnakeCaseMemberNames: Configuration = copy(
    transformMemberNames = Configuration.snakeCaseTransformation
  )

  def withKebabCaseMemberNames: Configuration = copy(
    transformMemberNames = Configuration.kebabCaseTransformation
  )

  def withSnakeCaseConstructorNames: Configuration = copy(
    transformConstructorNames = Configuration.snakeCaseTransformation
  )

  def withKebabCaseConstructorNames: Configuration = copy(
    transformConstructorNames = Configuration.kebabCaseTransformation
  )

  def withDefaults: Configuration = copy(useDefaults = true)
  def withDiscriminator(discriminator: String): Configuration = copy(discriminator = Some(discriminator))
}

final object Configuration {
  val default: Configuration = Configuration(Predef.identity, Predef.identity, false, None)

  val snakeCaseTransformation: String => String = _.replaceAll(
    "([A-Z]+)([A-Z][a-z])",
    "$1_$2"
  ).replaceAll("([a-z\\d])([A-Z])", "$1_$2").toLowerCase

  val kebabCaseTransformation: String => String = _.replaceAll(
    "([A-Z]+)([A-Z][a-z])",
    "$1-$2"
  ).replaceAll("([a-z\\d])([A-Z])", "$1-$2").toLowerCase
}

final object defaults {
  implicit val defaultGenericConfiguration: Configuration = Configuration.default
}
