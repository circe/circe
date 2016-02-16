package io.circe.generic

import io.circe.{ Decoder, Encoder }
import macrocompat.bundle
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

class JsonCodec extends scala.annotation.StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro JsonCodecMacros.jsonCodecAnnotationMacro
}

@bundle
private[generic] class JsonCodecMacros(val c: blackbox.Context) {
  import c.universe._

  def jsonCodecAnnotationMacro(annottees: Tree*): Tree = annottees match {
    case List(clsDef: ClassDef) if clsDef.mods hasFlag Flag.CASE =>
      q"""
       $clsDef
       object ${clsDef.name.toTermName} {
         ..${codec(clsDef.name)}
       }
       """
    case List(
      clsDef: ClassDef,
      q"object $objName extends { ..$objEarlyDefs } with ..$objParents { $objSelf => ..$objDefs }"
    ) if clsDef.mods hasFlag Flag.CASE =>
      q"""
       $clsDef
       object $objName extends { ..$objEarlyDefs} with ..$objParents { $objSelf =>
         ..${codec(clsDef.name)}
         ..$objDefs
       }
       """
    case _ => c.abort(c.enclosingPosition, "Invalid annotation target: must be a case class")
  }

  private[this] val DecoderClass = symbolOf[Decoder[_]]
  private[this] val EncoderClass = symbolOf[Encoder[_]]
  private[this] val semiautoObject = symbolOf[semiauto.type].asClass.module

  private[this] def codec(tpname: TypeName): List[Tree] = {
    val decodeNme = TermName("decode" + tpname.decodedName)
    val encodeNme = TermName("encode" + tpname.decodedName)
    List(
      q"""implicit val $decodeNme: $DecoderClass[$tpname] = $semiautoObject.deriveDecoder[$tpname]""",
      q"""implicit val $encodeNme: $EncoderClass[$tpname] = $semiautoObject.deriveEncoder[$tpname]"""
    )
  }
}
