package io.circe.generic.extras

/**
  * Configuration allowing customisation of the JSON produced when encoding, or expected when decoding. Can be used
  * with the [[ConfiguredJsonCodec]] annotation to allow customisation of the semi-automatic derivation.
  *
  * @param transformKeys Transforms the names of the keys in the JSON allowing, for example, formatting or case changes.
  * @param useDefaults Whether to allow default values as specified in any case-classes/ADT fields.
  * @param discriminator Optional key name that, when given, will be used to store the name of the type of an ADT in a
  *                      nested field with this name. If not given, the type is instead stored as a key under which the
  *                      contents of the ADT are stored as an object.
  * @param transformDiscriminators Transforms the value of the discriminator in the JSON allowing, for example,
  *                                formatting or case changes.
  */
final case class Configuration(transformKeys: String => String,
                               useDefaults: Boolean,
                               discriminator: Option[String],
                               transformDiscriminators: String => String) {
  def withSnakeCaseKeys: Configuration = copy(
    transformKeys = Configuration.snakeCaseTransformation
  )

  def withSnakeCaseDiscriminators: Configuration = copy(
    transformDiscriminators = Configuration.snakeCaseTransformation
  )

  def withDefaults: Configuration = copy(useDefaults = true)
  def withDiscriminator(discriminator: String): Configuration = copy(discriminator = Some(discriminator))
}

final object Configuration {
  val default: Configuration = Configuration(Predef.identity, false, None, Predef.identity)

  val snakeCaseTransformation: String => String = _.replaceAll(
    "([A-Z]+)([A-Z][a-z])",
    "$1_$2"
  ).replaceAll("([a-z\\d])([A-Z])", "$1_$2").toLowerCase
}

final object defaults {
  implicit val defaultGenericConfiguration: Configuration = Configuration.default
}
