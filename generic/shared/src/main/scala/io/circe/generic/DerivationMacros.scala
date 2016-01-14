package io.circe.generic

import io.circe.generic.config._
import macrocompat.bundle
import scala.reflect.macros.blackbox

@bundle
private[generic] class DerivationMacros(val c: blackbox.Context) {
  import c.universe._

  final val defaultConfiguration: Tree = q"_root_.io.circe.generic.config.Configuration.default"

  sealed class ParamExtractor[C: c.WeakTypeTag] {
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

  final object KeyTransformationType extends ParamExtractor[io.circe.generic.config.KeyTransformation]
  final object DiscriminatorType extends ParamExtractor[io.circe.generic.config.Discriminator]
  final object CaseObjectEncodingType extends ParamExtractor[io.circe.generic.config.CaseObjectEncoding]
  final object DefaultValuesType extends ParamExtractor[io.circe.generic.config.DefaultValues]

  /**
   * Create a [[io.circe.generic.config.Configuration]] instance tree from a type.
   */
  final def materializeConfiguration[C](implicit C: c.WeakTypeTag[C]): c.Expr[Configuration[C]] = {
    val config = C.tpe match {
      case KeyTransformationType(param) => q"$defaultConfiguration.copy(keyTransformation = $param)"
      case DiscriminatorType(param) => q"$defaultConfiguration.copy(discriminator = $param)"
      case CaseObjectEncodingType(param) => q"$defaultConfiguration.copy(caseObjectEncoding = $param)"
      case DefaultValuesType(param) => q"$defaultConfiguration.copy(defaultValues = $param)"

      case RefinedType(params, _) =>
        val keyTransformation = params.foldLeft[Tree](q"_root_.io.circe.generic.config.KeyIdentity") {
          case (last, KeyTransformationType(next)) => next
          case (last, _) => last
        }
        val discriminator = params.foldLeft[Tree](q"_root_.io.circe.generic.config.ObjectWrapper") {
          case (last, DiscriminatorType(next)) => next
          case (last, _) => last
        }
        val caseObjectEncoding = params.foldLeft[Tree](q"_root_.io.circe.generic.config.CaseObjectObject") {
          case (last, CaseObjectEncodingType(next)) => next
          case (last, _) => last
        }
        val defaultValues = params.foldLeft[Tree](q"_root_.io.circe.generic.config.NoDefaultValues") {
          case (last, DefaultValuesType(next)) => next
          case (last, _) => last
        }

        q"""
          _root_.io.circe.generic.config.Configuration(
            $keyTransformation,
            $discriminator,
            $caseObjectEncoding,
            $defaultValues
          )
        """
      case param => q"$defaultConfiguration"
    }

    c.Expr[Configuration[C]](q"$config.asInstanceOf[_root_.io.circe.generic.config.Configuration[$C]]")
  }

  final object NotExportedInstance {
    final val ExportPackageSymbol = typeOf[export.Export0[_]].typeSymbol.owner

    final def unapply(tree: Tree): Option[Tree] = tree match {
      case EmptyTree => None
      case q"(new $e(${_})).instance" => e.tpe match {
        case TypeRef(ThisType(ExportPackageSymbol), sym, _) => None
        case _ => Some(tree)
      }
      case other => Some(other)
    }
  }

  implicit final def notExportedImpl[A](implicit A: c.WeakTypeTag[A]): Tree =
    c.inferImplicitValue(A.tpe) match {
      case NotExportedInstance(instance) => q"new _root_.io.circe.generic.NotExported[$A]($instance)"
      case other => c.abort(c.enclosingPosition, "No valid instance")
    }
}
