package io.circe.generic.simple.util.macros

import io.circe.{ Decoder, Encoder }
import io.circe.export.Exported
import io.circe.generic.simple.decoding.DerivedDecoder
import io.circe.generic.simple.encoding.DerivedAsObjectEncoder
import scala.reflect.macros.blackbox

trait Lazy[+A] extends Serializable {
  val value: A
}

object Lazy {
  implicit def apply[A](implicit A: => A): Lazy[A] =
    new Lazy[A] {
      lazy val value = A
    }
  def lazily[A](implicit L: Lazy[A]): A = L.value
}

class ExportMacros(val c: blackbox.Context) {
  import c.universe._

  final def exportDecoder[D[x] <: DerivedDecoder[x], A](implicit
    D: c.WeakTypeTag[D[?]],
    A: c.WeakTypeTag[A]
  ): c.Expr[Exported[Decoder[A]]] = {
    val target = appliedType(D.tpe.typeConstructor, A.tpe)

    c.typecheck(q"_root_.io.circe.generic.simple.util.macros.Lazy.lazily[$target]", silent = true) match {
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

    c.typecheck(q"_root_.io.circe.generic.simple.util.macros.Lazy.lazily[$target]", silent = true) match {
      case EmptyTree => c.abort(c.enclosingPosition, s"Unable to infer value of type $target")
      case t         =>
        c.Expr[Exported[Encoder.AsObject[A]]](
          q"new _root_.io.circe.export.Exported($t: _root_.io.circe.Encoder.AsObject[$A])"
        )
    }
  }
}
