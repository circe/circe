package io.circe.generic

import io.circe.generic.util.macros.JsonCodecMacros
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

class JsonCodec(
  encodeOnly: Boolean = false,
  decodeOnly: Boolean = false
) extends scala.annotation.StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro GenericJsonCodecMacros.jsonCodecAnnotationMacro
}

private[generic] final class GenericJsonCodecMacros(val c: blackbox.Context) extends JsonCodecMacros {
  import c.universe._

  protected[this] def semiautoObj: Symbol = symbolOf[semiauto.type].asClass.module
  protected[this] def deriveMethodPrefix: String = "derive"

  def jsonCodecAnnotationMacro(annottees: Tree*): Tree = constructJsonCodec(annottees: _*)
}
