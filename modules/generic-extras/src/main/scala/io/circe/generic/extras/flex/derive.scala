package io.circe
package generic.extras.flex

import scala.reflect.macros.whitebox

import cats.Functor
import cats.sequence._

import shapeless.{HList, HNil, Poly2, ::, Witness, LabelledGeneric, =:!=, Default}
import shapeless.labelled.{FieldType, field}
import shapeless.ops.hlist.{LeftFolder, Zip}

import syntax.mapWith._

trait DefaultFields[A]
{
  type Out <: HList

  def create: Out
}

object DefaultFields
{
  type Aux[A, O <: HList] = DefaultFields[A] { type Out = O }

  implicit def hnil: DefaultFields.Aux[HNil, HNil] =
    new DefaultFields[HNil] {
      type Out = HNil
      def create: Out = HNil
    }

  implicit def hcons[K, V, L0 <: HList, OutH, OutT <: HList]
  (implicit tdf: DefaultFields.Aux[L0, OutT], key: Witness.Aux[K])
  : DefaultFields.Aux[FieldType[K, V] :: L0, FieldType[K, Auto[V]] :: OutT] =
    new DefaultFields[FieldType[K, V] :: L0] {
      type Out = FieldType[K, Auto[V]] :: OutT
      def create: Out = field[K](Auto[V]()) :: tdf.create
    }
}

object extractJson
extends Poly2
{
  implicit def extract[K <: Symbol: Witness.Aux, A, B](implicit E: Extract.Aux[A, B])
  : Case.Aux[FieldType[K, Extraction[B, A]], HCursor, Decoder.Result[FieldType[K, B]]]
  = at((f, cursor) => E[K](f.data)(cursor).map(field[K](_)))
}

trait replaceByKey
extends Poly2
{
  implicit def keep[K0, V0, K, V]
  (implicit ev: K0 =:!= K)
  : Case.Aux[FieldType[K0, V0], FieldType[K, V], FieldType[K0, V0]]
  = at((pre, post) => pre)
}

object replaceByKey
extends replaceByKey
{
  implicit def replace[K <: Symbol, A, V]
  (implicit ex: Extract.Aux[V, A])
  : Case.Aux[FieldType[K, Auto[A]], FieldType[K, V], FieldType[K, V]]
  = at((pre, post) => post)
}

object merge
extends Poly2
{
  implicit def withoutDefault[Rules <: HList, K, V0, V1]
  (implicit updater: LeftFolder.Aux[Rules, FieldType[K, Auto[V0]], replaceByKey.type, FieldType[K, V1]])
  : Case.Aux[(FieldType[K, Auto[V0]], None.type), Rules, FieldType[K, Extraction[V0, V1]]]
  = at { case ((df, dv), rules) =>
    field[K](Extraction[V0, V1](updater(rules, df)))
  }

  implicit def withDefault[Rules <: HList, K, V0, V1, D]
  (implicit updater: LeftFolder.Aux[Rules, FieldType[K, Auto[V0]], replaceByKey.type, FieldType[K, V1]])
  : Case.Aux[(FieldType[K, Auto[V0]], Some[D]), Rules, FieldType[K, Extraction[V0, DefaultValue[V1, D]]]]
  = at {
    case ((df, Some(dv)), rules) =>
      field[K](Extraction[V0, DefaultValue[V1, D]](DefaultValue(updater(rules, df), dv)))
  }
}

class DeriveDecoder[A]
{
  def apply[Repr <: HList, Rules <: HList, DF <: HList, DV <: HList, DD <: HList, Merged <: HList, Parsed <: HList]
  (custom: Rules)
    (implicit
      gen: LabelledGeneric.Aux[A, Repr],
      defaultFields: DefaultFields.Aux[Repr, DF],
      defaultValues: Default.Aux[A, DV],
      zipper: Zip.Aux[DF :: DV :: HNil, DD],
      merger: MapWith.Aux[merge.type, DD, Rules, Merged],
      extractor: MapWith.Aux[extractJson.type, Merged, HCursor, Parsed],
      seq: Sequencer.Aux[Parsed, Decoder.Result[Repr]]
      )
    : Decoder[A]
    = {
      val rules = defaultFields.create
        .zip(defaultValues())
        .mapWith(merge)(custom)
      Decoder.instance { cursor =>
        rules
          .mapWith(extractJson)(cursor)
          .sequence
          .map(LabelledGeneric[A].from)
      }
    }
}

trait ErrorHints
{
  val c: whitebox.Context

  import c.universe._

  object syms
  {
    val lgen = symbolOf[LabelledGeneric[_]].companion
    val decoder = symbolOf[Decoder[_]]
    val extract = symbolOf[Extract.Aux[_, _]]
    val fieldType = symbolOf[FieldType[_, _]]
    val path = symbolOf[Path]
    val ap = symbolOf[Ap[_, _, _]]
    val fmap = symbolOf[Fmap[_, _, _]]
    val value = symbolOf[Value[_, _]]
    val manual = symbolOf[Manual]
    val functor = symbolOf[Functor[X] forSome { type X[_] }]
  }

  def keyTagName = "shapeless.labelled.KeyTag"

  def taggedName = "shapeless.tag.Tagged"

  def isKeyTag(tpe: Type) = tpe.typeSymbol.fullName == keyTagName

  def isTagged(tpe: Type) = tpe.typeSymbol.fullName == taggedName

  object tag
  {
    def unapply(args: Type) = args match {
      case RefinedType(parents, _) => parents.find(isTagged)
      case _ => None
    }
  }

  object keyTag
  {
    def unapply(tpe: Type) = tpe.typeArgs match {
      case List(tag(args), _) if isKeyTag(tpe) => args.find(isTagged)
      case _ => None
    }
  }

  object extractRecord
  {
    def unapply(tpe: Type) = Some(tpe) collect {
      case RefinedType(List(actual, keyTag(key)), _) =>
        (actual, key)
      case TypeRef(pre, sym, List(tag(key), actual)) if sym == syms.fieldType =>
        (actual, key)
    }
  }

  object extractStringConstant
  {
    def unapply(tpe: Type) = tpe.typeArgs match {
      case List(RefinedType(parents, _), _) =>
        parents.find(isTagged)
          .flatMap(_.typeArgs.headOption)
          .collect { case ConstantType(Constant(a: String)) => Symbol(a) }
      case List(ConstantType(Constant(a: String))) =>
        Some(Symbol(a))
      case _ => None
    }
  }

  type SSymbol = scala.Symbol

  sealed trait KV
  {
    def key: SSymbol
  }

  case class Rule(key: SSymbol, tpe: Type)
  extends KV

  case class CCField(key: SSymbol, tpe: Type)
  extends KV

  def analyzeKV[A](tpe: Type, ctor: (SSymbol, Type) => A): A = {
    tpe match {
      case extractRecord(value, extractStringConstant(key)) => ctor(key, value)
      case _ => c.abort(c.enclosingPosition, s"invalid record element: $tpe")
    }
  }

  def ccFields(tpe: Type): List[CCField] = {
    val repr = tpe.member(TypeName("Repr")).asType.toType.dealias
    def extract(tpe: Type, result: List[CCField]): List[CCField] = {
      tpe.typeArgs match {
        case List(head, tail) => extract(tail, analyzeKV(head, CCField.apply) :: result)
        case Nil => result
      }
    }
    extract(repr, Nil).reverse
  }

  def check(tree: Tree) = c.typecheck(tree, silent = true)

  def inferImplicit(tpe: Tree): Either[String, Type] = {
    check(q"_root_.shapeless.lazily[$tpe]") match {
      case EmptyTree => Left(s"implicit $tpe not found")
      case t => Right(t.tpe)
    }
  }

  def checkDecoder(key: SSymbol, tpe: Type): Option[String] = {
    inferImplicit(tq"${syms.decoder}[$tpe]") match {
      case Left(err) => Some(s"you must define or import a decoder of `$tpe` for $key ($err)")
      case _ => None
    }
  }

  def mismatch(key: SSymbol, result: Type, target: Type) =
    s"you specified a map rule for $key with result type `$result`, but the field has type `$target`"

  def ruleError(key: SSymbol, target: Type, rule: Rule): Option[String] = {
    rule.tpe match {
      case TypeRef(_, sym, _) if sym == syms.path =>
        checkDecoder(key, target)
      case TypeRef(_, sym, _) if sym == syms.manual =>
        checkDecoder(key, target)
      case TypeRef(_, sym, List(pre, dec, result)) if sym == syms.ap =>
        if (result =:= target) ruleError(key, dec, Rule(key, pre))
        else Some(mismatch(key, result, target))
      case TypeRef(_, sym, List(pre, dec, result)) if sym == syms.fmap =>
        target.typeArgs match {
          case List(tpe) =>
            val cons = target.typeConstructor
            val fa = tq"${syms.functor}[${cons}]"
            inferImplicit(fa) match {
              case Left(err) => Some(s"you used an fmap rule with `$cons` for $key, but no `$fa` is in scope")
              case _ =>
                if (tpe =:= result) ruleError(key, dec, Rule(key, pre))
                else Some(mismatch(key, result, tpe))
            }
          case _ =>
            Some(s"cannot match shape of $target to fmap rule for $key with type $result")
        }
      case TypeRef(_, sym, List(pre, param)) if sym == syms.value =>
        check(q"${syms.lgen}[$target]") match {
          case EmptyTree => Some(s"you used a `value` rule for field $key which is not a case class")
          case t =>
            ccFields(t.tpe) match {
              case List(CCField(pkey, tpe)) if !(tpe =:= param) =>
                Some(s"you specified type `$param` for the value rule for $key," +
                  s" but the field $pkey of `$target` is `$tpe`")
              case List(a, b, _*) =>
                Some(s"you specified a value rule for $key, but `$target` has multiple fields")
              case _ => None
            }
        }
      case a => Some(s"unknown rule type $a")
    }
  }

  def checkRule(key: SSymbol, target: Type, rule: Rule) = {
    inferImplicit(tq"${syms.extract}[${rule.tpe}, ${target}]") match {
      case Left(err) => ruleError(key, target, rule).orElse(Some(err))
      case _ => None
    }
  }

  def checkError(main: Type, extra: Seq[c.Expr[Any]]) = {
    val errors = check(q"${syms.lgen}[$main]") match {
      case EmptyTree => List(s"$main is not a case class")
      case t =>
        val fields = ccFields(t.tpe)
        val extraFields =
          extra
            .map(_.tree)
            .map(c.typecheck(_))
            .map(_.tpe)
            .map(analyzeKV(_, Rule.apply))
            .toList
        (fields ++ extraFields)
          .groupBy(_.key)
          .values
          .map {
            case List(target: CCField, rule: Rule) => checkRule(target.key, target.tpe, rule)
            case List(target: CCField) => checkDecoder(target.key, target.tpe)
            case List(rule: Rule) => Some(s"you specified an extraction rule for nonexistent field `${rule.key.name}`")
            case _ => Some("invalid")
          }
          .collect { case Some(err) => err }
          .toList
    }
    val msg = s"deriving json decoder for `class $main` failed because"
    c.abort(c.enclosingPosition, (msg :: errors.map(a => s" • $a")).mkString("\n"))
  }
}

class DeriveMacro(val c: whitebox.Context)
extends ErrorHints
{
  import c.universe._

  val hnil = symbolOf[HNil].companion

  def derive[A](extra: c.Expr[Any]*)(implicit aType: WeakTypeTag[A]): c.Expr[Decoder[A]] = {
    val hlist = extra.toList.reverse.foldLeft(q"$hnil": Tree)((z, a) => q"$a :: $z")
    val tree =
      q"""
      _root_.io.circe.generic.extras.flex.deriveH[$aType]($hlist)
      """
    c.typecheck(tree, silent = true) match {
      case EmptyTree =>
        checkError(aType.tpe, extra)
      case t => c.Expr[Decoder[A]](t)
    }
  }
}
