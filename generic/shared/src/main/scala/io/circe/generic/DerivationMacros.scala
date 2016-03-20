package io.circe.generic

import io.circe.{ Decoder, ObjectEncoder }
import io.circe.export.Exported
import macrocompat.bundle
import scala.reflect.macros.whitebox

@bundle
class DerivationMacros(val c: whitebox.Context) {
  final def exportDecoderImpl[A: c.WeakTypeTag]: c.Expr[Exported[Decoder[A]]] = {
    import c.universe._

    val A = c.weakTypeOf[A]

    c.typecheck(q"_root_.shapeless.lazily[_root_.io.circe.generic.decoding.DerivedDecoder[$A]]", silent = true) match {
      case EmptyTree => c.abort(c.enclosingPosition, s"Unable to infer value of type DerivedDecoder[$A]")
      case t => c.Expr[Exported[Decoder[A]]](
        q"new _root_.io.circe.export.Exported($t: _root_.io.circe.Decoder[$A])"
      )
    }
  }

  final def exportEncoderImpl[A: c.WeakTypeTag]: c.Expr[Exported[ObjectEncoder[A]]] = {
    import c.universe._

    val A = c.weakTypeOf[A]

    c.typecheck(
      q"_root_.shapeless.lazily[_root_.io.circe.generic.encoding.DerivedObjectEncoder[$A]]",
      silent = true
    ) match {
      case EmptyTree => c.abort(c.enclosingPosition, s"Unable to infer value of type DerivedObjectEncoder[$A]")
      case t => c.Expr[Exported[ObjectEncoder[A]]](
        q"new _root_.io.circe.export.Exported($t: _root_.io.circe.ObjectEncoder[$A])"
      )
    }
  }
}
