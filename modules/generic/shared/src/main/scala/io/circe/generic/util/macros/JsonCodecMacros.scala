package io.circe.generic.util.macros

import io.circe.{ Decoder, Encoder, ObjectEncoder }
import macrocompat.bundle
import scala.reflect.macros.blackbox

@bundle
abstract class JsonCodecMacros {
  val c: blackbox.Context

  import c.universe._

  protected[this] def semiautoObj: Symbol

  private[this] def isCaseClassOrSealed(clsDef: ClassDef) =
    clsDef.mods.hasFlag(Flag.CASE) || clsDef.mods.hasFlag(Flag.SEALED)

  protected[this] final def constructJsonCodec(annottees: Tree*): Tree = annottees match {
    case List(clsDef: ClassDef) if isCaseClassOrSealed(clsDef) =>
      q"""
       $clsDef
       object ${ clsDef.name.toTermName } {
         ..${ codec(clsDef) }
       }
       """
    case List(
      clsDef: ClassDef,
      q"object $objName extends { ..$objEarlyDefs } with ..$objParents { $objSelf => ..$objDefs }"
    ) if isCaseClassOrSealed(clsDef) =>
      q"""
       $clsDef
       object $objName extends { ..$objEarlyDefs } with ..$objParents { $objSelf =>
         ..$objDefs
         ..${ codec(clsDef) }
       }
       """
    case _ => c.abort(c.enclosingPosition,
      "Invalid annotation target: must be a case class or a sealed trait/class")
  }

  private[this] val DecoderClass = typeOf[Decoder[_]].typeSymbol.asType
  private[this] val EncoderClass = typeOf[Encoder[_]].typeSymbol.asType
  private[this] val ObjectEncoderClass = typeOf[ObjectEncoder[_]].typeSymbol.asType

  private[this] def codec(clsDef: ClassDef): List[Tree] = {
    val tpname = clsDef.name
    val tparams = clsDef.tparams
    val decodeNme = TermName("decode" + tpname.decodedName)
    val encodeNme = TermName("encode" + tpname.decodedName)
    if (tparams.isEmpty) {
      val Type = tpname
      List(
        q"""implicit val $decodeNme: $DecoderClass[$Type] = $semiautoObj.deriveDecoder[$Type]""",
        q"""implicit val $encodeNme: $ObjectEncoderClass[$Type] = $semiautoObj.deriveEncoder[$Type]"""
      )
    } else {
      val tparamNames = tparams.map(_.name)
      def mkImplicitParams(typeSymbol: TypeSymbol) =
        tparamNames.map { tparamName =>
          val paramName = c.freshName(tparamName.toTermName)
          val paramType = tq"$typeSymbol[$tparamName]"
          q"$paramName: $paramType"
        }
      val decodeParams = mkImplicitParams(DecoderClass)
      val encodeParams = mkImplicitParams(EncoderClass)
      val Type = tq"$tpname[..$tparamNames]"
      List(
        q"""implicit def $decodeNme[..$tparams](implicit ..$decodeParams): $DecoderClass[$Type] =
           $semiautoObj.deriveDecoder[$Type]""",
        q"""implicit def $encodeNme[..$tparams](implicit ..$encodeParams): $ObjectEncoderClass[$Type] =
           $semiautoObj.deriveEncoder[$Type]"""
      )
    }
  }
}
