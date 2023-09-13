/*
 * Copyright 2023 circe
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

package io.circe.literal

import java.util.UUID
import scala.quoted.{ Expr, Quotes, Type }
import io.circe.{ Encoder, Json, KeyEncoder }

case class Replacement(val placeholder: String, argument: Expr[Any]) {
  def asJson(using q: Quotes): Expr[Json] = {
    import q.reflect.*
    argument match {
      case '{ $arg: t } => {
        arg.asTerm.tpe.widen.asType match {
          case '[t] =>
            Expr.summon[Encoder[t]] match {
              case Some(encoder) => '{ $encoder.apply($arg.asInstanceOf[t]) }
              case None          => report.errorAndAbort(s"could not find implicit Encoder for ${Type.show[t]}", arg)
            }
        }
      }
    }
  }

  def asKey(using q: Quotes): Expr[String] = {
    import q.reflect.*
    argument match {
      case '{ $arg: t } =>
        arg.asTerm.tpe.widen.asType match {
          case '[t] =>
            Expr.summon[KeyEncoder[t]] match {
              case Some(encoder) => '{ $encoder.apply($arg.asInstanceOf[t]) }
              case None => report.errorAndAbort(s"could not find implicit for ${Type.show[KeyEncoder[t]]}", arg)
            }
        }
    }
  }
}

object Replacement {
  private[this] final def generatePlaceholder(): String = UUID.randomUUID().toString

  def apply(stringParts: Seq[String], argument: Expr[Any]): Replacement = {

    val placeholder =
      Stream.continually(generatePlaceholder()).distinct.dropWhile(s => stringParts.exists(_.contains(s))).head

    new Replacement(placeholder, argument)
  }
}
