package io.circe.derivation

import scala.compiletime.{ codeOf, constValue, erasedValue, error, summonFrom, summonInline }
import scala.deriving.Mirror
import io.circe.{ Codec, Decoder, Encoder }
import scala.reflect.TypeTest

private[circe] inline final def summonLabels[T <: Tuple]: List[String] =
  inline erasedValue[T] match
    case _: EmptyTuple => Nil
    case _: (t *: ts)  => constValue[t].asInstanceOf[String] :: summonLabels[ts]

private[circe] inline final def summonLabelsRecursively[T <: Tuple]: List[String] =
  inline erasedValue[T] match
    case _: EmptyTuple => Nil
    case _: (t *: ts) =>
      inline summonInline[Mirror.Of[t]] match
        case mirror: Mirror.SumOf[t] =>
          Predef.println(s"sum of: ${constValue[mirror.MirroredLabel]}")
          summonLabelsRecursively[mirror.MirroredElemTypes] ::: summonLabelsRecursively[ts]
        case mirror: Mirror.ProductOf[t] =>
          Predef.println(s"product of: ${constValue[mirror.MirroredLabel]}")
          constValue[mirror.MirroredLabel] :: summonLabelsRecursively[ts]

private inline def summonTypeTestRecursively[T <: Tuple, A]: List[TypeTest[A, ?]] = inline erasedValue[T] match
    case _: (t *: ts)  =>
        inline summonInline[Mirror.Of[t]] match
            case mirror: Mirror.SumOf[t]     =>
                summonTypeTestRecursively[mirror.MirroredElemTypes, A] ::: summonTypeTestRecursively[ts, A]
            case mirror: Mirror.ProductOf[t] =>
                summon[TypeTest[A, t]] :: summonTypeTestRecursively[ts, A]
    case _: EmptyTuple => Nil

private[circe] inline final def summonEncoders[T <: Tuple](using Configuration): List[Encoder[_]] =
  inline erasedValue[T] match
    case _: EmptyTuple => Nil
    case _: (t *: ts)  => summonEncoder[t] :: summonEncoders[ts]

private[circe] inline final def summonEncoder[A](using Configuration): Encoder[A] =
  summonFrom {
    case encodeA: Encoder[A] => encodeA
    case _: Mirror.Of[A]     => ConfiguredEncoder.derived[A]
  }

private[circe] inline final def summonDecoders[T <: Tuple](using Configuration): List[Decoder[_]] =
  inline erasedValue[T] match
    case _: EmptyTuple => Nil
    case _: (t *: ts)  => summonDecoder[t] :: summonDecoders[ts]

private[circe] inline final def summonDecoder[A](using Configuration): Decoder[A] =
  summonFrom {
    case decodeA: Decoder[A] => decodeA
    case _: Mirror.Of[A]     => ConfiguredDecoder.derived[A]
  }

private[circe] inline def summonSingletonCases[T <: Tuple, A](inline typeName: Any): List[A] =
  inline erasedValue[T] match
    case _: EmptyTuple => Nil
    case _: (h *: t) =>
      inline summonInline[Mirror.Of[h]] match
        case m: Mirror.Singleton => 
          m.fromProduct(EmptyTuple).asInstanceOf[A] :: summonSingletonCases[t, A](typeName)
        case m: Mirror.SumOf[h]  => 
          summonSingletonCases[m.MirroredElemTypes, A](constValue[m.MirroredLabel]) ::: summonSingletonCases[t, A](typeName)
        case m: Mirror =>
          error("Enum " + codeOf(typeName) + " contains non singleton case " + codeOf(constValue[m.MirroredLabel]))
