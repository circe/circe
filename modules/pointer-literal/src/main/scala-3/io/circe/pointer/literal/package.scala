package io.circe.pointer

import scala.language.experimental.macros

package object literal {
  extension (inline sc: StringContext) {
    inline final def pointer(inline args: Any*): Pointer = ${ PointerLiteralMacros.pointerImpl('sc, 'args) }
  }
}
