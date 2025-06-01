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

package io.circe.literal

import scala.quoted.*
import io.circe.{ Encoder, Json, JsonNumber }
import org.typelevel.jawn.{ FContext, Facade, Parser }
import scala.util.{ Failure, Success }
import io.circe.literal.Replacement

object JsonLiteralMacros {
  def jsonImpl(sc: Expr[StringContext], args: Expr[Seq[Any]])(using q: Quotes): Expr[Json] = {
    import q.reflect.*
    val stringParts = sc match {
      case '{ StringContext($parts*) } => parts.valueOrAbort
    }

    val replacements = args match {
      case Varargs(argExprs) =>
        argExprs.map(Replacement(stringParts, _))
      case other => report.errorAndAbort("Invalid arguments for json literal.")
    }

    val jsonString = stringParts.zip(replacements.map(_.placeholder)).foldLeft("") {
      case (acc, (part, placeholder)) =>
        val qm = "\""
        s"$acc$part$qm$placeholder$qm"
    } + stringParts.last

    inline given Facade[Expr[Json]] with {

      private def toJsonKey(s: String): Expr[String] =
        replacements.find(_.placeholder == s).fold(Expr(s.toString))(_.asKey)
      private def toJsonString(s: String): Expr[Json] = replacements
        .find(_.placeholder == s)
        .fold { val strExpr = Expr(s.toString); '{ Json.fromString($strExpr) } }(_.asJson)
      def arrayContext(index: Int): FContext[Expr[Json]] = new FContext.NoIndexFContext[Expr[Json]] {
        private var values: Expr[List[Json]] = Expr(Nil)

        def isObj: Boolean = false

        def add(s: CharSequence): Unit = {
          val strExpr = toJsonString(s.toString)
          values = '{ $strExpr :: $values }
        }
        def add(v: Expr[Json]): Unit = values = '{ $v :: ${ values } }
        def finish(): Expr[Json] = '{ Json.arr($values.reverse*) }
      }
      def jfalse(index: Int): Expr[Json] = '{ Json.False }
      def jnull(index: Int): Expr[Json] = '{ Json.Null }
      inline def jnum(s: CharSequence, decIndex: Int, expIndex: Int, index: Int): Expr[Json] =
        val str = Expr(s.toString)
        '{ JsonNumber.fromString($str).map(Json.fromJsonNumber(_)).getOrElse(throw Exception("Invalid json number.")) }
      def jstring(s: CharSequence, index: Int): Expr[Json] = toJsonString(s.toString)
      def jtrue(index: Int): Expr[Json] = '{ Json.True }

      def objectContext(index: Int): FContext[Expr[Json]] = new FContext.NoIndexFContext[Expr[Json]] {
        private[this] var fields: Expr[List[(String, Json)]] = Expr(Nil)
        private[this] var key: String = null

        def isObj: Boolean = true
        def add(s: CharSequence): Unit = {
          if (key.eq(null)) {
            key = s.toString
          } else {
            val keyExpr = toJsonKey(key)
            val value = toJsonString(s.toString)
            fields = '{ ($keyExpr, $value) :: $fields }
            key = null
          }
        }
        def add(v: Expr[Json]): Unit = {
          val keyExpr = toJsonKey(key)
          fields = '{ ($keyExpr, $v) :: $fields }
          key = null
        }
        def finish(): Expr[Json] = '{ Json.obj($fields.reverse*) }
      }
      def singleContext(index: Int): FContext[Expr[Json]] = new FContext.NoIndexFContext[Expr[Json]] {
        private[this] var value: Expr[Json] = null

        def isObj: Boolean = false
        def add(s: CharSequence): Unit = value = toJsonString(s.toString)
        def add(v: Expr[Json]): Unit = value = v
        def finish(): Expr[Json] = value
      }
    }

    Parser.parseFromString[Expr[Json]](jsonString) match {
      case Success(jsonExpr) => jsonExpr
      case Failure(e)        =>
        report.errorAndAbort(e.toString)
    }
  }
}
