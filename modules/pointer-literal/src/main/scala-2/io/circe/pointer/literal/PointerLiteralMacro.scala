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

package io.circe.pointer.literal

import io.circe.pointer.Pointer
import scala.reflect.macros.blackbox

private class PointerLiteralMacros(val c: blackbox.Context) {
  final def pointerStringContext(args: c.Expr[Any]*): c.universe.Tree = {
    import c.universe._

    c.prefix.tree match {
      case Apply(_, Apply(_, parts) :: Nil) =>
        val stringParts: List[String] = parts.map {
          case Literal(Constant(part: String)) => part
          case _ =>
            c.abort(
              c.enclosingPosition,
              "A StringContext part for the pointer interpolator is not a string"
            )
        }

        if (Pointer.parse(stringParts.mkString("X")).isRight) {
          val input = args.zip(stringParts.tail).foldLeft(q"${stringParts.head}") {
            case (acc, (a, p)) =>
              q"""$acc + $a.toString.replaceAll("~1", "/").replaceAll("~0", "~").replaceAll("~", "~0").replaceAll("/", "~1") + $p"""
          }

          q"""_root_.io.circe.pointer.Pointer.parse($input)
            .asInstanceOf[_root_.scala.Right[_root_.scala.Nothing, _root_.io.circe.pointer.Pointer]].value
          """
        } else {
          c.abort(c.enclosingPosition, "Invalid JSON Pointer in interpolated string")
        }
      case _ => c.abort(c.enclosingPosition, "Invalid use of the pointer interpolator")
    }
  }
}
