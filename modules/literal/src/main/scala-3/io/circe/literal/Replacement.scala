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
