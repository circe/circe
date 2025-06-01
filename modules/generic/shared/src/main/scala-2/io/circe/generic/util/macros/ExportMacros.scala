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

package io.circe.generic.util.macros

import io.circe.{ Decoder, Encoder }
import io.circe.export.Exported
import io.circe.generic.decoding.DerivedDecoder
import io.circe.generic.encoding.DerivedAsObjectEncoder
import scala.reflect.macros.blackbox

class ExportMacros(val c: blackbox.Context) {
  import c.universe._

  final def exportDecoder[D[x] <: DerivedDecoder[x], A](implicit
    D: c.WeakTypeTag[D[?]],
    A: c.WeakTypeTag[A]
  ): c.Expr[Exported[Decoder[A]]] = {
    val target = appliedType(D.tpe.typeConstructor, A.tpe)

    c.typecheck(q"_root_.shapeless.lazily[$target]", silent = true) match {
      case EmptyTree => c.abort(c.enclosingPosition, s"Unable to infer value of type $target")
      case t         =>
        c.Expr[Exported[Decoder[A]]](
          q"new _root_.io.circe.export.Exported($t: _root_.io.circe.Decoder[$A])"
        )
    }
  }

  final def exportEncoder[E[x] <: DerivedAsObjectEncoder[x], A](implicit
    E: c.WeakTypeTag[E[?]],
    A: c.WeakTypeTag[A]
  ): c.Expr[Exported[Encoder.AsObject[A]]] = {
    val target = appliedType(E.tpe.typeConstructor, A.tpe)

    c.typecheck(q"_root_.shapeless.lazily[$target]", silent = true) match {
      case EmptyTree => c.abort(c.enclosingPosition, s"Unable to infer value of type $target")
      case t         =>
        c.Expr[Exported[Encoder.AsObject[A]]](
          q"new _root_.io.circe.export.Exported($t: _root_.io.circe.Encoder.AsObject[$A])"
        )
    }
  }
}
