package io.circe.generic

import io.circe.{ Decoder, ObjectEncoder }
import io.circe.export.Exported
import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context

/**
 * Fully automatic codec derivation.
 *
 * Importing the contents of this object provides [[io.circe.Decoder]] and [[io.circe.Encoder]]
 * instances for tuples, case classes (if all members have instances), "incomplete" case classes,
 * sealed trait hierarchies, etc.
 */
final object auto {
  implicit def exportDecoder[A]: Exported[Decoder[A]] = macro exportDecoderImpl[A]
  implicit def exportEncoder[A]: Exported[ObjectEncoder[A]] = macro exportEncoderImpl[A]

  def exportDecoderImpl[A](c: Context)(implicit A: c.WeakTypeTag[A]): c.Expr[Exported[Decoder[A]]] = {
    import c.universe._

    c.typecheck(q"_root_.shapeless.lazily[_root_.io.circe.generic.decoding.DerivedDecoder[$A]]", silent = true) match {
      case EmptyTree => c.abort(c.enclosingPosition, s"Unable to infer value of type DerivedDecoder[$A]")
      case t => c.Expr[Exported[Decoder[A]]](
        q"new _root_.io.circe.export.Exported($t: _root_.io.circe.Decoder[$A])"
      )
    }
  }

  def exportEncoderImpl[A](c: Context)(implicit A: c.WeakTypeTag[A]): c.Expr[Exported[ObjectEncoder[A]]] = {
    import c.universe._

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
