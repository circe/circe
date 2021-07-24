package io.circe.derivation

case class Configuration(
  transformNames: String => String = Predef.identity,
  useDefaults: Boolean = true,
  discriminator: Option[String] = None,
)
