package io.circe.pointer.literal

import io.circe.pointer.Pointer
import scala.reflect.macros.blackbox

private class PointerLiteralMacros(val c: blackbox.Context) {
  import c.universe._

  final def pointerStringContext(args: c.Expr[Any]*): Tree = c.prefix.tree match {
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
          case (acc, (a, p)) => q"""$acc + $a.toString.replaceAll("~", "~0").replaceAll("/", "~1") + $p"""
        }

        val t = q"""_root_.io.circe.pointer.Pointer.parse($input)
          .asInstanceOf[_root_.scala.Right[_root_.scala.Nothing, _root_.io.circe.pointer.Pointer]].value
        """

        t
      } else {
        c.abort(c.enclosingPosition, "Invalid JSON Pointer in interpolated string")
      }
    case _ => c.abort(c.enclosingPosition, "Invalid use of the pointer interpolator")
  }
}
