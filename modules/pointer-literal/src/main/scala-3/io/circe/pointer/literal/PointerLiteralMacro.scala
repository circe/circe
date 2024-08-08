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
import scala.quoted._
import scala.language.`3.0`

private object PointerLiteralMacros {

  final def pointerImpl(sc: Expr[StringContext], argsExpr: Expr[Seq[Any]])(using q: Quotes): Expr[Pointer] = {
    import q.reflect.*
    val stringParts: Seq[String] = sc match {
      case '{ StringContext($parts: _*) } => parts.valueOrAbort
    }

    if (Pointer.parse(stringParts.mkString("X")).isRight) {
      val args = argsExpr match {
        case Varargs(argExprs) => argExprs
        case other             => report.error("Invalid arguments for pointer literal."); Nil
      }
      val input = args.zip(stringParts.tail).foldLeft(Expr(stringParts.head)) {
        case (acc, (a, p)) =>
          val pExpr = Expr(p)
          '{
            $acc + $a.toString
              .replaceAll("~1", "/")
              .replaceAll("~0", "~")
              .replaceAll("~", "~0")
              .replaceAll("/", "~1") + $pExpr
          }
      }

      '{
        _root_.io.circe.pointer.Pointer
          .parse($input)
          .asInstanceOf[_root_.scala.Right[_root_.scala.Nothing, _root_.io.circe.pointer.Pointer]]
          .value
      }
    } else {
      q.reflect.report.error(s"Invalid JSON Pointer in interpolated string")
      '{ null }
    }
  }
}
