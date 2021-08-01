package io.circe.derivation

object Configuration:
  val default: Configuration = Configuration()
  val empty: Configuration = Configuration(transformNames = Predef.identity, useDefaults = false, discriminator = None)
case class Configuration(
  transformNames: String => String = Predef.identity,
  useDefaults: Boolean = true,
  discriminator: Option[String] = None,
):
  def withTransformNames(f: String => String): Configuration = this.copy(transformNames = f)
  def withSnakeCaseNames: Configuration = this.copy(transformNames = renaming.snakeCase)
  def withKebabCaseNames: Configuration = this.copy(transformNames = renaming.kebabCase)
  def withDefaults: Configuration = this.copy(useDefaults = true)
  def withoutDefaults: Configuration = this.copy(useDefaults = false)
  def withDiscriminator(discriminator: String): Configuration = this.copy(discriminator = Some(discriminator))
  def withoutDiscriminator: Configuration = this.copy(discriminator = None)
