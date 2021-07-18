package io.circe.generic.util.macros

import io.circe.{ Decoder, Encoder }
import io.circe.generic.decoding.ReprDecoder
import scala.annotation.tailrec
import scala.reflect.macros.blackbox
import shapeless.{ CNil, Coproduct, HList, HNil, Lazy }
import shapeless.labelled.KeyTag

abstract class DerivationMacros[RD[_], RE[_], RC[_], DD[_], DE[_], DC[_]] {
  val c: blackbox.Context

  import c.universe._

  protected[this] def RD: TypeTag[RD[_]]
  protected[this] def RE: TypeTag[RE[_]]
  protected[this] def RC: TypeTag[RC[_]]
  protected[this] def DD: TypeTag[DD[_]]
  protected[this] def DE: TypeTag[DE[_]]
  protected[this] def DC: TypeTag[DC[_]]

  protected[this] def hnilReprDecoder: Tree
  protected[this] def hnilReprCodec: Tree

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
    def fold[Z](namePrefix: String)(resolver: Type => Tree)(init: Z)(f: (Member, TermName, Z) => Z): (List[Tree], Z) = {
      val (instanceList, result) = underlying.foldRight((List.empty[(Type, (TermName, Tree))], init)) {
        case (member @ Member(label, _, valueType, _, _), (instanceList, acc)) =>
          val (newInstanceList, instanceName) =
            instanceList.find(_._1 =:= valueType) match {
              case Some(result) => (instanceList, result._2._1)
              case None =>
                val newName = TermName(s"$namePrefix$label").encodedName.toTermName
                val newInstance = resolver(valueType)

                ((valueType, (newName, newInstance)) :: instanceList, newName)
            }

          (newInstanceList, f(member, instanceName, acc))
      }

      val instanceDefs = instanceList.map {
        case (_, (instanceName, instance)) => q"private[this] val $instanceName = $instance"
      }

      (instanceDefs, result)
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
        case RefinedType(parents, scope) =>
          parents.reverse match {
            case TypeRef(lt, KeyTagSym, List(tagType, taggedFieldType)) :: refs
                if lt =:= ShapelessLabelledType && internal.refinedType(refs.reverse, scope) =:= taggedFieldType =>
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
        case EmptyTree          => resolveInstance(rest)(tpe)
        case instance if lazily => q"{ $instance }.value"
        case instance           => instance
      }
    case Nil => fail(tpe)
  }

  val ReprDecoderUtils = symbolOf[ReprDecoder.type].asClass.module

  private[this] def hlistDecoderParts(members: Members): (List[c.Tree], (c.Tree, c.Tree)) =
    members.fold("circeGenericDecoderFor")(
      resolveInstance(List((typeOf[Decoder[_]], false)))
    )((q"$ReprDecoderUtils.hnilResult": Tree, q"$ReprDecoderUtils.hnilResultAccumulating": Tree)) {
      case (Member(label, nameTpe, tpe, _, accTail), instanceName, (acc, accAccumulating)) =>
        (
          q"""
        $ReprDecoderUtils.consResults[
          _root_.io.circe.Decoder.Result,
          $nameTpe,
          $tpe,
          $accTail
        ](
          ${decodeField(label, instanceName)},
          $acc
        )(_root_.io.circe.Decoder.resultInstance)
      """,
          q"""
        $ReprDecoderUtils.consResults[
          _root_.io.circe.Decoder.AccumulatingResult,
          $nameTpe,
          $tpe,
          $accTail
        ](
          ${decodeFieldAccumulating(label, instanceName)},
          $accAccumulating
        )(_root_.io.circe.Decoder.accumulatingResultInstance)
      """
        )
    }

  private[this] val cnilResult: Tree = q"""
    _root_.scala.util.Left[_root_.io.circe.DecodingFailure, _root_.shapeless.CNil](
      _root_.io.circe.DecodingFailure("JSON decoding to CNil should never happen", c.history)
    ): _root_.scala.util.Either[_root_.io.circe.DecodingFailure, _root_.shapeless.CNil]
  """

  private[this] val cnilResultAccumulating: Tree = q"""
    _root_.cats.data.Validated.invalidNel[_root_.io.circe.DecodingFailure, _root_.shapeless.CNil](
      _root_.io.circe.DecodingFailure("JSON decoding to CNil should never happen", c.history)
    )
  """

  private[this] def coproductDecoderParts(members: Members): (List[c.Tree], (c.Tree, c.Tree)) =
    members.fold("circeGenericDecoderFor")(
      resolveInstance(List((typeOf[Decoder[_]], false), (DD.tpe, true)))
    )((cnilResult, cnilResultAccumulating)) {
      case (Member(label, nameTpe, tpe, current, accTail), instanceName, (acc, accAccumulating)) =>
        (
          q"""
        ${decodeSubtype(label, instanceName)} match {
          case _root_.scala.Some(result) => result match {
            case _root_.scala.util.Right(v) =>
              _root_.scala.util.Right($ReprDecoderUtils.injectLeftValue[$nameTpe, $tpe, $accTail](v))
            case _root_.scala.util.Left(err) => _root_.scala.util.Left(err)
          }
          case _root_.scala.None => $acc match {
            case _root_.scala.util.Right(v) => _root_.scala.util.Right(_root_.shapeless.Inr(v))
            case _root_.scala.util.Left(err) => _root_.scala.util.Left(err)
          }
        }
      """,
          q"""
        ${decodeSubtypeAccumulating(label, instanceName)} match {
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

    if (!isHList && !isCoproduct) fail(R.tpe)
    else {
      val members = Members.fromType(R.tpe)

      if (isHList && members.underlying.isEmpty) q"$hnilReprDecoder"
      else {
        val (instanceDefs, (result, resultAccumulating)) =
          if (isHList) hlistDecoderParts(members) else coproductDecoderParts(members)
        val instanceType = appliedType(RD.tpe.typeConstructor, List(R.tpe))

        q"""
          new $instanceType {
            ..$instanceDefs

            final def $decodeMethodName(
              ...${fullDecodeMethodArgs(R.tpe)}
            ): _root_.io.circe.Decoder.Result[$R] = $result

            final override def $decodeAccumulatingMethodName(
              ...${fullDecodeAccumulatingMethodArgs(R.tpe)}
            ): _root_.io.circe.Decoder.AccumulatingResult[$R] = $resultAccumulating
          }: $instanceType
        """
      }
    }
  }

  protected[this] def hlistEncoderParts(members: Members): (List[c.Tree], c.Tree) = {
    val (instanceDefs, (pattern, fields)) =
      members.fold("circeGenericEncoderFor")(resolveInstance(List((typeOf[Encoder[_]], false))))(
        (pq"_root_.shapeless.HNil": Tree, List.empty[Tree])
      ) {
        case (Member(label, _, tpe, _, _), instanceName, (patternAcc, fieldsAcc)) =>
          val currentName = TermName(s"circeGenericHListBindingFor$label").encodedName.toTermName

          (
            pq"_root_.shapeless.::($currentName, $patternAcc)",
            encodeField(label, instanceName, currentName) :: fieldsAcc
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
    val (instanceDefs, patternAndCase) = members.fold("circeGenericEncoderFor")(
      resolveInstance(List((typeOf[Encoder[_]], false), (DE.tpe, true)))
    )(
      cq"""_root_.shapeless.Inr(_) => _root_.scala.sys.error("Cannot encode CNil")"""
    ) {
      case (Member(label, _, tpe, _, _), instanceName, acc) =>
        val inrName = TermName(s"circeGenericInrBindingFor$label").encodedName.toTermName
        val inlName = TermName(s"circeGenericInlBindingFor$label").encodedName.toTermName

        cq"""
          _root_.shapeless.Inr($inrName) => $inrName match {
            case _root_.shapeless.Inl($inlName) =>
              ${encodeSubtype(label, instanceName, inlName)}
            case $acc
          }
        """
    }

    (instanceDefs, q"_root_.shapeless.Inr(a) match { case $patternAndCase }")
  }

  protected[this] def constructEncoder[R](implicit R: c.WeakTypeTag[R]): c.Tree = {
    val isHList = R.tpe <:< HListType
    val isCoproduct = !isHList && R.tpe <:< CoproductType

    if (!isHList && !isCoproduct) fail(R.tpe)
    else {
      val members = Members.fromType(R.tpe)

      val (instanceDefs, instanceImpl) =
        if (isHList) hlistEncoderParts(members) else coproductEncoderParts(members)

      val instanceType = appliedType(RE.tpe.typeConstructor, List(R.tpe))

      q"""
        new $instanceType {
          ..$instanceDefs

          final def $encodeMethodName(...${fullEncodeMethodArgs(R.tpe)}): _root_.io.circe.JsonObject = $instanceImpl
        }: $instanceType
      """
    }
  }

  protected[this] def constructCodec[R](implicit R: c.WeakTypeTag[R]): c.Tree = {
    val isHList = R.tpe <:< HListType
    val isCoproduct = !isHList && R.tpe <:< CoproductType

    if (!isHList && !isCoproduct) fail(R.tpe)
    else {
      val members = Members.fromType(R.tpe)

      if (isHList && members.underlying.isEmpty) q"$hnilReprCodec"
      else {
        val (encoderInstanceDefs, (result, resultAccumulating)) =
          if (isHList) hlistDecoderParts(members) else coproductDecoderParts(members)

        val (decoderInstanceDefs, encoderInstanceImpl) =
          if (isHList) hlistEncoderParts(members) else coproductEncoderParts(members)

        val instanceType = appliedType(RC.tpe.typeConstructor, List(R.tpe))

        q"""
          new $instanceType {
            ..$encoderInstanceDefs
            ..$decoderInstanceDefs

            final def $encodeMethodName(...${fullEncodeMethodArgs(R.tpe)}): _root_.io.circe.JsonObject =
              $encoderInstanceImpl

            final def $decodeMethodName(
              ...${fullDecodeMethodArgs(R.tpe)}
            ): _root_.io.circe.Decoder.Result[$R] = $result

            final override def $decodeAccumulatingMethodName(
              ...${fullDecodeAccumulatingMethodArgs(R.tpe)}
            ): _root_.io.circe.Decoder.AccumulatingResult[$R] = $resultAccumulating
          }: $instanceType
        """
      }
    }
  }
}
