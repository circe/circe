package io.circe

import io.circe.literal.JsonLiteralMacros

package object literal {
  extension (inline sc: StringContext) {
    inline final def json(inline args: Any*): Json = ${ JsonLiteralMacros.jsonImpl('sc, 'args) }
  }
}
