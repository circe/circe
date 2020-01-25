package io.circe.generic

import io.circe.generic.codec.{ DerivedAsObjectCodec, ReprAsObjectCodec }
import io.circe.generic.decoding.{ DerivedDecoder, ReprDecoder }
import io.circe.generic.encoding.{ DerivedAsObjectEncoder, ReprAsObjectEncoder }
import io.circe.generic.util.macros.DerivationMacros
import scala.reflect.macros.whitebox

class Deriver(val c: whitebox.Context)
    extends DerivationMacros[
      ReprDecoder,
      ReprAsObjectEncoder,
      ReprAsObjectCodec,
      DerivedDecoder,
      DerivedAsObjectEncoder,
      DerivedAsObjectCodec
    ] {
  import c.universe._

  def deriveDecoder[R: c.WeakTypeTag]: c.Expr[ReprDecoder[R]] = c.Expr[ReprDecoder[R]](constructDecoder[R])
  def deriveEncoder[R: c.WeakTypeTag]: c.Expr[ReprAsObjectEncoder[R]] =
    c.Expr[ReprAsObjectEncoder[R]](constructEncoder[R])
  def deriveCodec[R: c.WeakTypeTag]: c.Expr[ReprAsObjectCodec[R]] = c.Expr[ReprAsObjectCodec[R]](constructCodec[R])

  protected[this] val RD: TypeTag[ReprDecoder[_]] = c.typeTag
  protected[this] val RE: TypeTag[ReprAsObjectEncoder[_]] = c.typeTag
  protected[this] val RC: TypeTag[ReprAsObjectCodec[_]] = c.typeTag
  protected[this] val DD: TypeTag[DerivedDecoder[_]] = c.typeTag
  protected[this] val DE: TypeTag[DerivedAsObjectEncoder[_]] = c.typeTag
  protected[this] val DC: TypeTag[DerivedAsObjectCodec[_]] = c.typeTag

  protected[this] val hnilReprDecoder: Tree = q"_root_.io.circe.generic.decoding.ReprDecoder.hnilReprDecoder"
  protected[this] val hnilReprCodec: Tree = q"_root_.io.circe.generic.codec.ReprAsObjectCodec.hnilReprCodec"

  protected[this] val decodeMethodName: TermName = TermName("apply")
  protected[this] val decodeAccumulatingMethodName: TermName = TermName("decodeAccumulating")
  protected[this] val encodeMethodName: TermName = TermName("encodeObject")

  protected[this] def decodeField(name: String, decode: TermName): Tree =
    q"$decode.tryDecode(c.downField($name))"

  protected[this] def decodeFieldAccumulating(name: String, decode: TermName): Tree =
    q"$decode.tryDecodeAccumulating(c.downField($name))"

  protected[this] def decodeSubtype(name: String, decode: TermName): Tree = q"""
    {
      val result = c.downField($name)

      if (result.succeeded) _root_.scala.Some($decode.tryDecode(result)) else _root_.scala.None
    }
  """

  protected[this] def decodeSubtypeAccumulating(name: String, decode: TermName): Tree = q"""
    {
      val result = c.downField($name)

      if (result.succeeded) _root_.scala.Some($decode.tryDecodeAccumulating(result)) else _root_.scala.None
    }
  """

  protected[this] def encodeField(name: String, encode: TermName, value: TermName): Tree =
    q"($name, $encode($value))"

  protected[this] def encodeSubtype(name: String, encode: TermName, value: TermName): Tree =
    q"_root_.io.circe.JsonObject.singleton($name, $encode($value))"
}
