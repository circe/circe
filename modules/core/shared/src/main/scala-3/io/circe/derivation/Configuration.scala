package io.circe.derivation

object Configuration:
  val default: Configuration = Configuration()

/**
 * Configuration allowing customization of the JSON produced when encoding, or expected when decoding.
 *
 * @param transformMemberNames Transforms the names of any case class members in the JSON allowing, for example,
 *                             formatting or case changes.
 * @param useDefaults Whether to allow default values as specified for any case-class members.
 * @param discriminator Optional key name that, when given, will be used to store the name of the constructor of an ADT
 *                      in a nested field with this name. If not given, the name is instead stored as a key under which
 *                      the contents of the ADT are stored as an object.
 * @param transformConstructorNames Transforms the value of any constructor names in the JSON allowing, for example,
 *                                  formatting or case changes.
 * @param strictDecoding Whether to fail when superfluous fields are found.
 */
case class Configuration(
  transformMemberNames: String => String = Predef.identity,
  transformConstructorNames: String => String = Predef.identity,
  useDefaults: Boolean = false,
  discriminator: Option[String] = None,
  strictDecoding: Boolean = false
):
  def withTransformMemberNames(f: String => String): Configuration = copy(transformMemberNames = f)
  def withSnakeCaseMemberNames: Configuration = withTransformMemberNames(renaming.snakeCase)
  def withScreamingSnakeCaseMemberNames: Configuration = withTransformMemberNames(renaming.screamingSnakeCase)
  def withKebabCaseMemberNames: Configuration = withTransformMemberNames(renaming.kebabCase)
  def withPascalCaseMemberNames: Configuration = withTransformMemberNames(renaming.pascalCase)

  def withTransformConstructorNames(f: String => String): Configuration = copy(transformConstructorNames = f)
  def withSnakeCaseConstructorNames: Configuration = withTransformConstructorNames(renaming.snakeCase)
  def withScreamingSnakeCaseConstructorNames: Configuration = withTransformConstructorNames(renaming.screamingSnakeCase)
  def withKebabCaseConstructorNames: Configuration = withTransformConstructorNames(renaming.kebabCase)
  def withPascalCaseConstructorNames: Configuration = withTransformConstructorNames(renaming.pascalCase)

  def withDefaults: Configuration = copy(useDefaults = true)
  def withoutDefaults: Configuration = copy(useDefaults = false)

  def withDiscriminator(discriminator: String): Configuration = copy(discriminator = Some(discriminator))
  def withoutDiscriminator: Configuration = copy(discriminator = None)

  def withStrictDecoding: Configuration = copy(strictDecoding = true)
  def withoutStrictDecoding: Configuration = copy(strictDecoding = false)
