/*
 * Copyright 2024 circe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.circe.generic

import io.circe.generic.util.macros.JsonCodecMacros
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
