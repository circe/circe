package io.circe.generic

import io.circe.{ Decoder, Encoder, ObjectEncoder }
import macrocompat.bundle
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

class JsonCodec extends scala.annotation.StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro JsonCodecMacros.jsonCodecAnnotationMacro
}

@bundle
private[generic] class JsonCodecMacros(val c: blackbox.Context) {
  import c.universe._

  private[this] def isCaseClassOrSealed(clsDef: ClassDef) =
    clsDef.mods.hasFlag(Flag.CASE) || clsDef.mods.hasFlag(Flag.SEALED)

  def jsonCodecAnnotationMacro(annottees: Tree*): Tree = annottees match {
    case List(clsDef: ClassDef) if isCaseClassOrSealed(clsDef) =>
      q"""
       $clsDef
       object ${clsDef.name.toTermName} {
         ..${codec(clsDef)}
       }
       """
    case List(
      clsDef: ClassDef,
      q"object $objName extends { ..$objEarlyDefs } with ..$objParents { $objSelf => ..$objDefs }"
    ) if isCaseClassOrSealed(clsDef) =>
      q"""
       $clsDef
       object $objName extends { ..$objEarlyDefs} with ..$objParents { $objSelf =>
         ..$objDefs
         ..${codec(clsDef)}
       }
       """
    case _ => c.abort(c.enclosingPosition,
      "Invalid annotation target: must be a case class or a sealed trait/class")
  }

  private[this] val DecoderClass = symbolOf[Decoder[_]]
  private[this] val EncoderClass = symbolOf[Encoder[_]]
  private[this] val ObjectEncoderClass = symbolOf[ObjectEncoder[_]]
  private[this] val semiautoObj = symbolOf[semiauto.type].asClass.module

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
