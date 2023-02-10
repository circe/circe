package io.circe.derivation

import scala.quoted.*
import scala.deriving.*
import scala.compiletime.constValue

/**
 * Original code by Dmytro Mitin, with slight modifications by Simão Martins.
 * See: https://stackoverflow.com/questions/68421043/type-class-derivation-accessing-default-values
 */

trait Default[T] extends Serializable:
  type Out <: Tuple
  def defaults: Out

  def defaultAt(index: Int): Option[Any] = defaults match
    case _: EmptyTuple           => None
    case defaults: NonEmptyTuple => defaults(index).asInstanceOf[Option[Any]]

object Default:
  transparent inline given mkDefault[T](using mirror: Mirror.Of[T]): Default[T] =
    new Default[T]:
      type Out = Tuple.Map[mirror.MirroredElemTypes, Option]
      lazy val defaults: Out =
        val size = constValue[Tuple.Size[mirror.MirroredElemTypes]]
        getDefaults[T](size).asInstanceOf[Out]

  inline def getDefaults[T](inline s: Int): Tuple = ${ getDefaultsImpl[T]('s) }

  def getDefaultsImpl[T](s: Expr[Int])(using Quotes, Type[T]): Expr[Tuple] =
    import quotes.reflect.*

    val n = s.asTerm.underlying.asInstanceOf[Literal].constant.value.asInstanceOf[Int]

    val companion = TypeRepr.of[T].typeSymbol.companionModule

    val expressions: List[Expr[Option[Any]]] = List.tabulate(n) { i =>
      val termOpt =
        companion.declaredMethod(s"$$lessinit$$greater$$default$$${i + 1}").headOption.map(Select(Ref(companion), _))

      termOpt match
        case None     => Expr(None)
        case Some(et) => '{ Some(${ et.asExpr }) }
    }
    Expr.ofTupleFromSeq(expressions)
