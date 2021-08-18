package io.circe.derivation

object Configuration:
  val default: Configuration = Configuration()
  val empty: Configuration = Configuration(useDefaults = false)
case class Configuration(
  transformNames: String => String = Predef.identity,
  useDefaults: Boolean = true,
  discriminator: Option[String] = None,
  strictDecoding: Boolean = false,
):
  def withTransformNames(f: String => String): Configuration = this.copy(transformNames = f)
  def withSnakeCaseNames: Configuration = this.copy(transformNames = renaming.snakeCase)
  def withScreamingSnakeCaseNames: Configuration = this.copy(transformNames = renaming.screamingSnakeCase)
  def withKebabCaseNames: Configuration = this.copy(transformNames = renaming.kebabCase)
  def withDefaults: Configuration = this.copy(useDefaults = true)
  def withoutDefaults: Configuration = this.copy(useDefaults = false)
  def withDiscriminator(discriminator: String): Configuration = this.copy(discriminator = Some(discriminator))
  def withoutDiscriminator: Configuration = this.copy(discriminator = None)
  def withStrictDecoding: Configuration = this.copy(strictDecoding = true)
  def withoutStrictDecoding: Configuration = this.copy(strictDecoding = false)
