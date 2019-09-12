package io.circe.generic.util.macros

import io.circe.{ Codec, Decoder, Encoder }
import scala.reflect.macros.blackbox

abstract class JsonCodecMacros {
  val c: blackbox.Context

  import c.universe._

  protected[this] def semiautoObj: Symbol
  protected[this] def deriveMethodPrefix: String

  private[this] def isCaseClassOrSealed(clsDef: ClassDef) =
    clsDef.mods.hasFlag(Flag.CASE) || clsDef.mods.hasFlag(Flag.SEALED)

  protected[this] final def constructJsonCodec(annottees: Tree*): Tree = annottees match {
    case List(clsDef: ClassDef) if isCaseClassOrSealed(clsDef) =>
      q"""
       $clsDef
       object ${clsDef.name.toTermName} {
         ${codec(clsDef)}
       }
       """
    case List(
        clsDef: ClassDef,
        q"..$mods object $objName extends { ..$objEarlyDefs } with ..$objParents { $objSelf => ..$objDefs }"
        ) if isCaseClassOrSealed(clsDef) =>
      q"""
       $clsDef
       $mods object $objName extends { ..$objEarlyDefs } with ..$objParents { $objSelf =>
         ..$objDefs
         ${codec(clsDef)}
       }
       """
    case _ => c.abort(c.enclosingPosition, "Invalid annotation target: must be a case class or a sealed trait/class")
  }

  private[this] val DecoderClass = typeOf[Decoder[_]].typeSymbol.asType
  private[this] val EncoderClass = typeOf[Encoder[_]].typeSymbol.asType
  private[this] val AsObjectEncoderClass = typeOf[Encoder.AsObject[_]].typeSymbol.asType
  private[this] val AsObjectCodecClass = typeOf[Codec.AsObject[_]].typeSymbol.asType

  private[this] val macroName: Tree = {
    c.prefix.tree match {
      case Apply(Select(New(name), _), _) => name
      case _                              => c.abort(c.enclosingPosition, "Unexpected macro application")
    }
  }

  private[this] val codecType: JsonCodecType = {
    c.prefix.tree match {
      case q"new ${`macroName` }()"                  => JsonCodecType.Both
      case q"new ${`macroName` }(encodeOnly = true)" => JsonCodecType.EncodeOnly
      case q"new ${`macroName` }(decodeOnly = true)" => JsonCodecType.DecodeOnly
      // format: off
      case _ => c.abort(c.enclosingPosition, s"Unsupported arguments supplied to @$macroName")
      // format: on
    }
  }

  private[this] def codec(clsDef: ClassDef): Tree = {
    val tpname = clsDef.name
    val tparams = clsDef.tparams
    val decodeName = TermName("decode" + tpname.decodedName)
    val encodeName = TermName("encode" + tpname.decodedName)
    val codecName = TermName("codecFor" + tpname.decodedName)
    def deriveName(suffix: String) = TermName(deriveMethodPrefix + suffix)
    val (decoder, encoder, codec) = if (tparams.isEmpty) {
      val Type = tpname
      (
        q"""implicit val $decodeName: $DecoderClass[$Type] = $semiautoObj.${deriveName("Decoder")}[$Type]""",
        q"""implicit val $encodeName: $AsObjectEncoderClass[$Type] = $semiautoObj.${deriveName("Encoder")}[$Type]""",
        q"""implicit val $codecName: $AsObjectCodecClass[$Type] = $semiautoObj.${deriveName("Codec")}[$Type]"""
      )
    } else {
      val tparamNames = tparams.map(_.name)
      def mkImplicitParams(prefix: String, typeSymbol: TypeSymbol) =
        tparamNames.zipWithIndex.map {
          case (tparamName, i) =>
            val paramName = TermName(s"$prefix$i")
            val paramType = tq"$typeSymbol[$tparamName]"
            q"$paramName: $paramType"
        }
      val decodeParams = mkImplicitParams("decode", DecoderClass)
      val encodeParams = mkImplicitParams("encode", EncoderClass)
      val Type = tq"$tpname[..$tparamNames]"
      (
        q"""implicit def $decodeName[..$tparams](implicit ..$decodeParams): $DecoderClass[$Type] =
            $semiautoObj.${deriveName("Decoder")}[$Type]""",
        q"""implicit def $encodeName[..$tparams](implicit ..$encodeParams): $AsObjectEncoderClass[$Type] =
            $semiautoObj.${deriveName("Encoder")}[$Type]""",
        q"""implicit def $codecName[..$tparams](implicit
            ..${decodeParams ++ encodeParams}
          ): $AsObjectCodecClass[$Type] =
            $semiautoObj.${deriveName("Codec")}[$Type]"""
      )
    }
    codecType match {
      case JsonCodecType.Both       => codec
      case JsonCodecType.DecodeOnly => decoder
      case JsonCodecType.EncodeOnly => encoder
    }
  }
}

private sealed trait JsonCodecType
private object JsonCodecType {
  case object Both extends JsonCodecType
  case object DecodeOnly extends JsonCodecType
  case object EncodeOnly extends JsonCodecType
}
