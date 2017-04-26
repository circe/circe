package io.circe.generic.extras

final case class Configuration(transformKeys: String => String, useDefaults: Boolean, discriminator: Option[String]) {
  def withSnakeCaseKeys: Configuration = copy(
    transformKeys = Configuration.snakeCaseTransformation
  )

  def withDefaults: Configuration = copy(useDefaults = true)
  def withDiscriminator(discriminator: String): Configuration = copy(discriminator = Some(discriminator))
}

final object Configuration {
  val default: Configuration = Configuration(Predef.identity, false, None)

  val snakeCaseTransformation: String => String = _.replaceAll(
    "([A-Z]+)([A-Z][a-z])",
    "$1_$2"
  ).replaceAll("([a-z\\d])([A-Z])", "$1_$2").toLowerCase
}

final object defaults {
  implicit val defaults: Configuration = Configuration.default
}
