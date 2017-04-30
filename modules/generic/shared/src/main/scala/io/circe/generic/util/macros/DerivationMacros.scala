package io.circe.generic.util.macros

import io.circe.{ Decoder, Encoder }
import io.circe.generic.decoding.ReprDecoder
import macrocompat.bundle
import scala.annotation.tailrec
import scala.collection.immutable.Map
import scala.reflect.macros.whitebox
import shapeless.{ CNil, Coproduct, HList, HNil, Lazy }
import shapeless.labelled.KeyTag

@bundle
abstract class DerivationMacros[RD[_], RE[_], DD[_], DE[_]] {
  val c: whitebox.Context

  import c.universe._

  protected[this] def RD: TypeTag[RD[_]]
  protected[this] def RE: TypeTag[RE[_]]
  protected[this] def DD: TypeTag[DD[_]]
  protected[this] def DE: TypeTag[DE[_]]

  protected[this] def hnilReprDecoder: Tree

  protected[this] def decodeMethodName: TermName
  protected[this] def decodeMethodArgs: List[Tree] = Nil

  protected[this] def decodeAccumulatingMethodName: TermName
  protected[this] def decodeAccumulatingMethodArgs: List[Tree] = decodeMethodArgs

  protected[this] def encodeMethodName: TermName
  protected[this] def encodeMethodArgs: List[Tree] = Nil

  protected[this] def decodeField(name: String, decode: TermName): Tree
  protected[this] def decodeFieldAccumulating(name: String, decode: TermName): Tree

  protected[this] def decodeSubtype(name: String, decode: TermName): Tree
  protected[this] def decodeSubtypeAccumulating(name: String, decode: TermName): Tree

  protected[this] def encodeField(name: String, encode: TermName, value: TermName): Tree
  protected[this] def encodeSubtype(name: String, encode: TermName, value: TermName): Tree

  private[this] def fullDecodeMethodArgs(tpe: Type): List[List[Tree]] =
    List(q"c: _root_.io.circe.HCursor") :: (if (decodeMethodArgs.isEmpty) Nil else List(decodeMethodArgs))

  private[this] def fullDecodeAccumulatingMethodArgs(tpe: Type): List[List[Tree]] =
    List(q"c: _root_.io.circe.HCursor") :: (
      if (decodeAccumulatingMethodArgs.isEmpty) Nil else List(decodeAccumulatingMethodArgs)
    )

  private[this] def fullEncodeMethodArgs(tpe: Type): List[List[Tree]] =
    List(q"a: $tpe") :: (if (encodeMethodArgs.isEmpty) Nil else List(encodeMethodArgs))

  /**
   * Crash the attempted derivation because of a failure related to the
   * specified type.
   *
   * Note that these failures are not generally visible to users.
   */
  private[this] def fail(tpe: Type): Nothing =
    c.abort(c.enclosingPosition, s"Cannot generically derive instance: $tpe")

  /**
   * Represents an element at the head of a `shapeless.HList` or
   * `shapeless.Coproduct`.
   */
  private[this] case class Member(label: String, keyType: Type, valueType: Type, acc: Type, accTail: Type)

  /**
   * Represents an `shapeless.HList` or `shapeless.Coproduct` type in a way
   * that's more convenient to work with.
   */
  private[this] class Members(val underlying: List[Member]) {
    /**
     * Fold over the elements of this (co-)product while accumulating instances
     * of some type class for each.
     */
    def fold[Z](resolver: Type => Tree)(init: Z)(f: (Member, TermName, Z) => Z): (List[Tree], Z) = {
      val (instanceMap, result) = underlying.foldRight((Map.empty[Type, (TermName, Tree)], init)) {
        case (member @ Member(_, _, valueType, _, _), (instanceMap, acc)) =>
          val (instanceName, instance) = instanceMap.getOrElse(valueType, (TermName(c.freshName), resolver(valueType)))
          val newInstances = instanceMap.updated(valueType, (instanceName, instance))

          (newInstances, f(member, instanceName, acc))
      }

      val instanceDefs = instanceMap.values.map {
        case (instanceName, instance) => q"private[this] val $instanceName = $instance"
      }

      (instanceDefs.toList, result)
    }
  }

  private[this] val HListType: Type = typeOf[HList]
  private[this] val CoproductType: Type = typeOf[Coproduct]

  private[this] object Members {
    private[this] val ShapelessSym = typeOf[HList].typeSymbol.owner
    private[this] val HNilSym = typeOf[HNil].typeSymbol
    private[this] val HConsSym = typeOf[shapeless.::[_, _]].typeSymbol
    private[this] val CNilSym = typeOf[CNil].typeSymbol
    private[this] val CConsSym = typeOf[shapeless.:+:[_, _]].typeSymbol
    private[this] val ShapelessLabelledType = typeOf[shapeless.labelled.type]
    private[this] val KeyTagSym = typeOf[KeyTag[_, _]].typeSymbol
    private[this] val ShapelessTagType = typeOf[shapeless.tag.type]
    private[this] val ScalaSymbolType = typeOf[scala.Symbol]

    case class Entry(label: String, keyType: Type, valueType: Type)

    object Entry {
      // We need this import for the `RefinedType` constructor in Scala 2.12.
      import compat._

      def unapply(tpe: Type): Option[(String, Type, Type)] = tpe.dealias match {
        /**
         * Before Scala 2.12 the `RefinedType` extractor returns the field type
         * (including any refinements) as the first result in the list.
         */
        case RefinedType(List(fieldType, TypeRef(lt, KeyTagSym, List(tagType, taggedFieldType))), _)
          if lt =:= ShapelessLabelledType && fieldType =:= taggedFieldType =>
            tagType.dealias match {
              case RefinedType(List(st, TypeRef(tt, ts, ConstantType(Constant(fieldKey: String)) :: Nil)), _)
                if st =:= ScalaSymbolType && tt =:= ShapelessTagType =>
                  Some((fieldKey, tagType, fieldType))
              case _ => None
            }
        /**
         * In Scala 2.12 the `RefinedType` extractor returns a refined type with
         * each individual refinement as a separate element in the list.
         */
        case RefinedType(parents, scope) => parents.reverse match {
          case TypeRef(lt, KeyTagSym, List(tagType, taggedFieldType)) :: refs
            if lt =:= ShapelessLabelledType && RefinedType(refs.reverse, scope) =:= taggedFieldType =>
              tagType.dealias match {
                case RefinedType(List(st, TypeRef(tt, ts, ConstantType(Constant(fieldKey: String)) :: Nil)), _)
                  if st =:= ScalaSymbolType && tt =:= ShapelessTagType =>
                    Some((fieldKey, tagType, taggedFieldType))
                case _ => None
              }
          case _ => None
        }
        case _ => None
      }
    }

    def fromType(tpe: Type): Members = tpe.dealias match {
      case TypeRef(ThisType(ShapelessSym), HNilSym | CNilSym, Nil) => new Members(Nil)
      case acc @ TypeRef(ThisType(ShapelessSym), HConsSym | CConsSym, List(fieldType, tailType)) =>
        fieldType match {
          case Entry(label, keyType, valueType) =>
            new Members(Member(label, keyType, valueType, acc, tailType) :: fromType(tailType).underlying)
          case _ => fail(tpe)
        }
      case _ => fail(tpe)
    }
  }

  @tailrec
  private[this] def resolveInstance(tcs: List[(Type, Boolean)])(tpe: Type): Tree = tcs match {
    case (tc, lazily) :: rest =>
      val applied = c.universe.appliedType(tc.typeConstructor, List(tpe))
      val target = if (lazily) c.universe.appliedType(typeOf[Lazy[_]].typeConstructor, List(applied)) else applied
      val inferred = c.inferImplicitValue(target, silent = true)

      inferred match {
        case EmptyTree => resolveInstance(rest)(tpe)
        case instance if lazily => q"{ $instance }.value"
        case instance => instance
      }
    case Nil => fail(tpe)
  }

  val ReprDecoderUtils = symbolOf[ReprDecoder.type].asClass.module

  private[this] def hlistDecoderParts(members: Members): (List[c.Tree], (c.Tree, c.Tree)) = members.fold(
    resolveInstance(List((typeOf[Decoder[_]], false)))
  )((q"$ReprDecoderUtils.hnilResult": Tree, q"$ReprDecoderUtils.hnilResultAccumulating": Tree)) {
    case (Member(name, nameTpe, tpe, _, accTail), instanceName, (acc, accAccumulating)) => (
      q"""
        $ReprDecoderUtils.consResults[
          _root_.io.circe.Decoder.Result,
          $nameTpe,
          $tpe,
          $accTail
        ](
          ${ decodeField(name, instanceName) },
          $acc
        )(_root_.io.circe.Decoder.resultInstance)
      """,
      q"""
        $ReprDecoderUtils.consResults[
          _root_.io.circe.AccumulatingDecoder.Result,
          $nameTpe,
          $tpe,
          $accTail
        ](
          ${ decodeFieldAccumulating(name, instanceName) },
          $accAccumulating
        )(_root_.io.circe.AccumulatingDecoder.resultInstance)
      """
    )
  }

  private[this] val cnilResult: Tree = q"""
    _root_.scala.util.Left[_root_.io.circe.DecodingFailure, _root_.shapeless.CNil](
      _root_.io.circe.DecodingFailure("CNil", c.history)
    ): _root_.scala.util.Either[_root_.io.circe.DecodingFailure, _root_.shapeless.CNil]
  """

  private[this] val cnilResultAccumulating: Tree = q"""
    _root_.cats.data.Validated.invalidNel[_root_.io.circe.DecodingFailure, _root_.shapeless.CNil](
      _root_.io.circe.DecodingFailure("CNil", c.history)
    )
  """

  private[this] def coproductDecoderParts(members: Members): (List[c.Tree], (c.Tree, c.Tree)) = members.fold(
    resolveInstance(List((typeOf[Decoder[_]], false), (DD.tpe, true)))
  )((cnilResult, cnilResultAccumulating)) {
    case (Member(name, nameTpe, tpe, current, accTail), instanceName, (acc, accAccumulating)) => (
      q"""
        ${ decodeSubtype(name, instanceName) } match {
          case _root_.scala.Some(result) => result.right.map(v =>
           $ReprDecoderUtils.injectLeftValue[$nameTpe, $tpe, $accTail](v)
          )
          case _root_.scala.None => $acc.right.map(_root_.shapeless.Inr(_))
        }
      """,
      q"""
        ${ decodeSubtypeAccumulating(name, instanceName) } match {
          case _root_.scala.Some(result) => result.map(v =>
            $ReprDecoderUtils.injectLeftValue[$nameTpe, $tpe, $accTail](v)
          )
          case _root_.scala.None => $accAccumulating.map(_root_.shapeless.Inr(_))
        }
      """
    )
  }

  protected[this] def constructDecoder[R](implicit R: c.WeakTypeTag[R]): c.Tree = {
    val isHList = R.tpe <:< HListType
    val isCoproduct = !isHList && R.tpe <:< CoproductType

    if (!isHList && !isCoproduct) fail(R.tpe) else {
      val members = Members.fromType(R.tpe)

      if (isHList && members.underlying.isEmpty) q"$hnilReprDecoder" else {
        val (instanceDefs, (result, resultAccumulating)) =
          if (isHList) hlistDecoderParts(members) else coproductDecoderParts(members)
        val instanceType = appliedType(RD.tpe.typeConstructor, List(R.tpe))

        q"""
          new $instanceType {
            ..$instanceDefs

            final def $decodeMethodName(
              ...${ fullDecodeMethodArgs(R.tpe) }
            ): _root_.io.circe.Decoder.Result[$R] = $result

            final override def $decodeAccumulatingMethodName(
              ...${ fullDecodeAccumulatingMethodArgs(R.tpe) }
            ): _root_.io.circe.AccumulatingDecoder.Result[$R] = $resultAccumulating
          }: $instanceType
        """
      }
    }
  }

  protected[this] def hlistEncoderParts(members: Members): (List[c.Tree], c.Tree) = {
    val (instanceDefs, (pattern, fields)) = members.fold(resolveInstance(List((typeOf[Encoder[_]], false))))(
      (pq"_root_.shapeless.HNil": Tree, List.empty[Tree])
    ) {
      case (Member(name, _, tpe, _, _), instanceName, (patternAcc, fieldsAcc)) =>
        val currentName = TermName(c.freshName)

        (
          pq"_root_.shapeless.::($currentName, $patternAcc)",
          encodeField(name, instanceName, currentName) :: fieldsAcc
        )
    }

    (
      instanceDefs,
      q"""
        a match {
          case $pattern =>
            _root_.io.circe.JsonObject.fromIterable(_root_.scala.collection.immutable.Vector(..$fields))
        }
      """
    )
  }

  private[this] def coproductEncoderParts(members: Members): (List[c.Tree], c.Tree) = {
    val (instanceDefs, patternAndCase) = members.fold(
      resolveInstance(List((typeOf[Encoder[_]], false), (DE.tpe, true)))
    )(
      cq"""_root_.shapeless.Inr(_) => _root_.scala.sys.error("Cannot encode CNil")"""
    ) {
      case (Member(name, _, tpe, _, _), instanceName, acc) =>
        val tailName = TermName(c.freshName)
        val currentName = TermName(c.freshName)

        cq"""
          _root_.shapeless.Inr($tailName) => $tailName match {
            case _root_.shapeless.Inl($currentName) =>
              ${ encodeSubtype(name, instanceName, currentName) }
            case $acc
          }
        """
    }

    (instanceDefs, q"_root_.shapeless.Inr(a) match { case $patternAndCase }")
  }

  protected[this] def constructEncoder[R](implicit R: c.WeakTypeTag[R]): c.Tree = {
    val isHList = R.tpe <:< HListType
    val isCoproduct = !isHList && R.tpe <:< CoproductType

    if (!isHList && !isCoproduct) fail(R.tpe) else {
      val members = Members.fromType(R.tpe)

      val (instanceDefs, instanceImpl) =
        if (isHList) hlistEncoderParts(members) else coproductEncoderParts(members)

      val instanceType = appliedType(RE.tpe.typeConstructor, List(R.tpe))

      q"""
        new $instanceType {
          ..$instanceDefs

          final def $encodeMethodName(...${ fullEncodeMethodArgs(R.tpe) }): _root_.io.circe.JsonObject = $instanceImpl
        }: $instanceType
      """
    }
  }
}
