package io.circe.generic

import io.circe.generic.config._
import macrocompat.bundle
import scala.language.experimental.macros
import scala.reflect.macros.whitebox

final class NotExported[A](final val value: A)

final object NotExported {
  implicit final def notExported[A]: NotExported[A] = macro DerivationMacros.notExportedImpl[A]
}

@bundle
class DerivationMacros(val c: whitebox.Context) {
  import c.universe._

  final val defaultConfiguration: Tree = q"_root_.io.circe.generic.config.Configuration.default"

  class ParamExtractor[C: c.WeakTypeTag] {
    final def companionTree(tpe: Type): Option[Tree] = tpe.typeSymbol.companion match {
      case NoSymbol => None
      case sym => Some(Ident(sym))
    }

    final def unapply(tpe: Type): Option[Tree] = tpe match {
      case RefinedType(_, _) => None
      case unrefined if tpe <:< weakTypeOf[C] => Some(companionTree(tpe).getOrElse(q"new $tpe"))
      case _ => None
    }
  }

  object KeyTransformationType extends ParamExtractor[io.circe.generic.config.KeyTransformation]
  object DiscriminatorType extends ParamExtractor[io.circe.generic.config.Discriminator]
  object CaseObjectEncodingType extends ParamExtractor[io.circe.generic.config.CaseObjectEncoding]

  /**
   * Create a [[io.circe.generic.config.Configuration]] instance tree from a type.
   */
  final def materializeConfiguration[C](implicit C: c.WeakTypeTag[C]): c.Expr[Configuration[C]] = {
    val config = C.tpe match {
      case KeyTransformationType(param) => q"$defaultConfiguration.copy(keyTransformation = $param)"
      case DiscriminatorType(param) => q"$defaultConfiguration.copy(discriminator = $param)"
      case CaseObjectEncodingType(param) =>
        q"$defaultConfiguration.copy(caseObjectEncoding = $param)"

      case RefinedType(params, _) =>
        val keyTransformation =
          params.foldLeft[Tree](q"_root_.io.circe.generic.config.KeyIdentity") {
            case (last, KeyTransformationType(next)) => next
            case (last, _) => last
          }
        val discriminator = params.foldLeft[Tree](q"_root_.io.circe.generic.config.ObjectWrapper") {
          case (last, DiscriminatorType(next)) => next
          case (last, _) => last
        }
        val caseObjectEncoding =
          params.foldLeft[Tree](q"_root_.io.circe.generic.config.CaseObjectObject") {
            case (last, CaseObjectEncodingType(next)) => next
            case (last, _) => last
          }

        q"""
          _root_.io.circe.generic.config.Configuration(
            $keyTransformation,
            $discriminator,
            $caseObjectEncoding
          )
        """
      case param => q"$defaultConfiguration"
    }

    c.Expr[Configuration[C]](
      q"$config.asInstanceOf[_root_.io.circe.generic.config.Configuration[$C]]"
    )
  }

  object NotExportedInstance {
    val ExportPackageSymbol = typeOf[export.Export0[_]].typeSymbol.owner

    final def unapply(tree: Tree): Option[Tree] = tree match {
      case EmptyTree => None
      case q"(new $e(${_})).instance" =>
        e.tpe match {
          case TypeRef(ThisType(ExportPackageSymbol), sym, _) => None
          case _ => Some(tree)
        }
      case other => Some(other)
    }
  }

  implicit def notExportedImpl[A](implicit A: c.WeakTypeTag[A]): Tree =
    c.inferImplicitValue(A.tpe) match {
      case NotExportedInstance(instance) =>
        q"new _root_.io.circe.generic.NotExported[$A]($instance)"
      case other => c.abort(c.enclosingPosition, "No valid instance")
    }
}
