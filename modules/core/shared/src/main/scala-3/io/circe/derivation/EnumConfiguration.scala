package io.circe.derivation

object EnumConfiguration:
  val default: EnumConfiguration = EnumConfiguration()
final case class EnumConfiguration(
  decodeTransformNames: String => String = Predef.identity,
  encodeTransformNames: String => String = Predef.identity,
):
  def withDecodeTransformNames(f: String => String): EnumConfiguration = this.copy(decodeTransformNames = f)
  def withEncodeTransformNames(f: String => String): EnumConfiguration = this.copy(encodeTransformNames = f)
