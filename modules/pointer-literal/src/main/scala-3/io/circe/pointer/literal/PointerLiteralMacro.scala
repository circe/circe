package io.circe.pointer.literal

import io.circe.pointer.Pointer
import scala.quoted._
import scala.language.`3.0`

private object PointerLiteralMacros {

  final def pointerImpl(sc: Expr[StringContext], argsExpr: Expr[Seq[Any]])(using q: Quotes): Expr[Pointer] = {
    import q.reflect.*
    val stringParts: Seq[String] = sc match {
      case '{StringContext($parts:_*)} => parts.valueOrAbort
    }

    if (Pointer.parse(stringParts.mkString("X")).isRight) {
      val args = argsExpr match { 
      case Varargs(argExprs) => argExprs
      case other => report.error("Invalid arguments for pointer literal.");Nil
    }
      val input = args.zip(stringParts.tail).foldLeft(Expr(stringParts.head)) {
        case (acc, (a, p)) => 
          val pExpr = Expr(p)
          '{$acc + $a.toString.replaceAll("~", "~0").replaceAll("/", "~1") + $pExpr }
      }

      val t = '{_root_.io.circe.pointer.Pointer.parse($input)
        .asInstanceOf[_root_.scala.Right[_root_.scala.Nothing, _root_.io.circe.pointer.Pointer]].value
      }

      t
    } else {
      q.reflect.report.error(s"Invalid JSON Pointer in interpolated string")
      '{ null }
    }
  }
}
