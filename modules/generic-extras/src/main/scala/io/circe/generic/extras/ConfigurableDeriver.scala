package io.circe.generic.extras

import io.circe.generic.extras.codec.{ ConfiguredAsObjectCodec, ReprAsObjectCodec }
import io.circe.generic.extras.decoding.{ ConfiguredDecoder, ReprDecoder }
import io.circe.generic.extras.encoding.{ ConfiguredAsObjectEncoder, ReprAsObjectEncoder }
import io.circe.generic.util.macros.DerivationMacros
import scala.reflect.macros.whitebox

class ConfigurableDeriver(val c: whitebox.Context)
    extends DerivationMacros[
      ReprDecoder,
      ReprAsObjectEncoder,
      ReprAsObjectCodec,
      ConfiguredDecoder,
      ConfiguredAsObjectEncoder,
      ConfiguredAsObjectCodec
    ] {
  import c.universe._

  def deriveConfiguredDecoder[R: c.WeakTypeTag]: c.Expr[ReprDecoder[R]] =
    c.Expr[ReprDecoder[R]](constructDecoder[R])
  def deriveConfiguredEncoder[R: c.WeakTypeTag]: c.Expr[ReprAsObjectEncoder[R]] =
    c.Expr[ReprAsObjectEncoder[R]](constructEncoder[R])
  def deriveConfiguredCodec[R: c.WeakTypeTag]: c.Expr[ReprAsObjectCodec[R]] =
    c.Expr[ReprAsObjectCodec[R]](constructCodec[R])

  protected[this] val RD: TypeTag[ReprDecoder[_]] = c.typeTag
  protected[this] val RE: TypeTag[ReprAsObjectEncoder[_]] = c.typeTag
  protected[this] val RC: TypeTag[ReprAsObjectCodec[_]] = c.typeTag
  protected[this] val DD: TypeTag[ConfiguredDecoder[_]] = c.typeTag
  protected[this] val DE: TypeTag[ConfiguredAsObjectEncoder[_]] = c.typeTag
  protected[this] val DC: TypeTag[ConfiguredAsObjectCodec[_]] = c.typeTag

  protected[this] val hnilReprDecoder: Tree = q"_root_.io.circe.generic.extras.decoding.ReprDecoder.hnilReprDecoder"
  protected[this] val hnilReprCodec: Tree = q"_root_.io.circe.generic.extras.codec.ReprAsObjectCodec.hnilReprCodec"

  protected[this] val decodeMethodName: TermName = TermName("configuredDecode")
  protected[this] val decodeAccumulatingMethodName: TermName = TermName("configuredDecodeAccumulating")

  protected[this] override def decodeMethodArgs: List[Tree] = List(
    q"transformMemberNames: (_root_.java.lang.String => _root_.java.lang.String)",
    q"transformConstructorNames: (_root_.java.lang.String => _root_.java.lang.String)",
    q"defaults: _root_.scala.collection.immutable.Map[_root_.java.lang.String, _root_.scala.Any]",
    q"discriminator: _root_.scala.Option[_root_.java.lang.String]"
  )

  protected[this] def encodeMethodName: TermName = TermName("configuredEncodeObject")
  protected[this] override def encodeMethodArgs: List[Tree] = List(
    q"transformMemberNames: (_root_.java.lang.String => _root_.java.lang.String)",
    q"transformConstructorNames: (_root_.java.lang.String => _root_.java.lang.String)",
    q"discriminator: _root_.scala.Option[_root_.java.lang.String]"
  )

  protected[this] def decodeField(name: String, decode: TermName): Tree = q"""
    orDefault(
      c.downField(transformMemberNames($name)),
      $decode,
      $name,
      defaults
    )
  """

  protected[this] def decodeFieldAccumulating(name: String, decode: TermName): Tree = q"""
    orDefaultAccumulating(
      c.downField(transformMemberNames($name)),
      $decode,
      $name,
      defaults
    )
  """

  protected[this] def decodeSubtype(name: String, decode: TermName): Tree = q"""
    withDiscriminator(
      $decode,
      c,
      transformConstructorNames($name),
      discriminator
    )
  """

  protected[this] def decodeSubtypeAccumulating(name: String, decode: TermName): Tree = q"""
    withDiscriminatorAccumulating(
      $decode,
      c,
      transformConstructorNames($name),
      discriminator
    )
  """

  protected[this] def encodeField(name: String, encode: TermName, value: TermName): Tree =
    q"(transformMemberNames($name), $encode($value))"

  protected[this] def encodeSubtype(name: String, encode: TermName, value: TermName): Tree =
    q"addDiscriminator($encode, $value, transformConstructorNames($name), discriminator)"
}
