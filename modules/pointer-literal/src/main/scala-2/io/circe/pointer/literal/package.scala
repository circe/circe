package io.circe.pointer

import scala.language.experimental.macros

package object literal {
  implicit final class PointerStringContext(sc: StringContext) {
    final def pointer(args: Any*): Pointer = macro PointerLiteralMacros.pointerStringContext
  }
}
