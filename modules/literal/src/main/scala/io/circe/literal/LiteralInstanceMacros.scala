package io.circe.literal

import macrocompat.bundle
import scala.reflect.macros.whitebox

@bundle
class LiteralInstanceMacros(val c: whitebox.Context) {
  import c.universe._

  final def decodeLiteralString[S <: String: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(lit: String)) =>
        val name = s"""String("$lit")"""

        q"""
          _root_.io.circe.Decoder.instance[$sType] { c =>
            if (c.value.asString.exists(_ == $lit)) {
              _root_.scala.util.Right[_root_.io.circe.DecodingFailure, $sType]($lit: $sType)
            } else {
              _root_.scala.util.Left(
                _root_.io.circe.DecodingFailure($name, c.history)
              )
            }
          }
        """
    }

  final def decodeLiteralDouble[S <: Double: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(lit: Double)) =>
        val name = s"""Double($lit)"""

        q"""
          _root_.io.circe.Decoder.instance[$sType] { c =>
            if (c.value.asNumber.map(_.toDouble).exists(_ == $lit)) {
              _root_.scala.util.Right[_root_.io.circe.DecodingFailure, $sType]($lit: $sType)
            } else {
              _root_.scala.util.Left(
                _root_.io.circe.DecodingFailure($name, c.history)
              )
            }
          }
        """
    }

  final def decodeLiteralFloat[S <: Float: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(lit: Float)) =>
        val name = s"""Float($lit)"""

        q"""
          _root_.io.circe.Decoder.instance[$sType] { c =>
            if (c.value.asNumber.map(_.toDouble).exists(s => s.toFloat == $lit)) {
              _root_.scala.util.Right[_root_.io.circe.DecodingFailure, $sType]($lit: $sType)
            } else {
              _root_.scala.util.Left(
                _root_.io.circe.DecodingFailure($name, c.history)
              )
            }
          }
        """
    }

  final def decodeLiteralLong[S <: Long: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(lit: Long)) =>
        val name = s"""Long($lit)"""

        q"""
          _root_.io.circe.Decoder.instance[$sType] { c =>
            if (c.value.asNumber.flatMap(_.toLong).exists(_ == $lit)) {
              _root_.scala.util.Right[_root_.io.circe.DecodingFailure, $sType]($lit: $sType)
            } else {
              _root_.scala.util.Left(
                _root_.io.circe.DecodingFailure($name, c.history)
              )
            }
          }
        """
    }

  final def decodeLiteralInt[S <: Int: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(lit: Int)) =>
        val name = s"""Int($lit)"""

        q"""
          _root_.io.circe.Decoder.instance[$sType] { c =>
            if (c.value.asNumber.flatMap(_.toInt).exists(_ == $lit)) {
              _root_.scala.util.Right[_root_.io.circe.DecodingFailure, $sType]($lit: $sType)
            } else {
              _root_.scala.util.Left(
                _root_.io.circe.DecodingFailure($name, c.history)
              )
            }
          }
        """
    }

  final def decodeLiteralChar[S <: Char: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(lit: Char)) =>
        val name = s"""Char($lit)"""

        q"""
          _root_.io.circe.Decoder.instance[$sType] { c =>
            if (c.value.asString.exists(s => s.length == 1 && s.charAt(0) == $lit)) {
              _root_.scala.util.Right[_root_.io.circe.DecodingFailure, $sType]($lit: $sType)
            } else {
              _root_.scala.util.Left(
                _root_.io.circe.DecodingFailure($name, c.history)
              )
            }
          }
        """
    }

  final def decodeLiteralBoolean[S <: Boolean: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(lit: Boolean)) =>
        val name = s"""Boolean($lit)"""

        q"""
          _root_.io.circe.Decoder.instance[$sType] { c =>
            if (c.value.asBoolean.exists(_ == $lit)) {
              _root_.scala.util.Right[_root_.io.circe.DecodingFailure, $sType]($lit: $sType)
            } else {
              _root_.scala.util.Left(
                _root_.io.circe.DecodingFailure($name, c.history)
              )
            }
          }
        """
    }

  final def encodeLiteralString[S <: String: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(lit: String)) =>
        q"_root_.io.circe.Encoder.apply[_root_.java.lang.String].contramap[$sType](_root_.scala.Predef.identity)"
    }

  final def encodeLiteralDouble[S <: Double: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(lit: Double)) =>
        q"_root_.io.circe.Encoder.apply[_root_.scala.Double].contramap[$sType](_root_.scala.Predef.identity)"
    }

  final def encodeLiteralFloat[S <: Float: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(lit: Float)) =>
        q"_root_.io.circe.Encoder.apply[_root_.scala.Float].contramap[$sType](_root_.scala.Predef.identity)"
    }

  final def encodeLiteralLong[S <: Long: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(lit: Long)) =>
        q"_root_.io.circe.Encoder.apply[_root_.scala.Long].contramap[$sType](_root_.scala.Predef.identity)"
    }

  final def encodeLiteralInt[S <: Int: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(lit: Int)) =>
        q"_root_.io.circe.Encoder.apply[_root_.scala.Int].contramap[$sType](_root_.scala.Predef.identity)"
    }

  final def encodeLiteralChar[S <: Char: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(lit: Char)) =>
        q"_root_.io.circe.Encoder.apply[_root_.scala.Char].contramap[$sType](_root_.scala.Predef.identity)"
    }

  final def encodeLiteralBoolean[S <: Boolean: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(lit: Boolean)) =>
        q"_root_.io.circe.Encoder.apply[_root_.scala.Boolean].contramap[$sType](_root_.scala.Predef.identity)"
    }
}
