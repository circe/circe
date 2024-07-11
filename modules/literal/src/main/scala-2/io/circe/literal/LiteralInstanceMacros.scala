/*
 * Copyright 2024 circe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.circe.literal

import scala.reflect.macros.whitebox

class LiteralInstanceMacros(val c: whitebox.Context) {
  import c.universe._

  final def decodeLiteralString[S <: String: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(lit: String)) =>
        q"""
          _root_.io.circe.Decoder.instance[$sType] { c =>
            if (c.value.asString.exists(_ == $lit)) {
              _root_.scala.util.Right[_root_.io.circe.DecodingFailure, $sType]($lit: $sType)
            } else {
              _root_.scala.util.Left(
                _root_.io.circe.DecodingFailure(
                  "Couldn't decode '" + $lit + "' string literal", c.history)
              )
            }
          }
        """
    }

  final def decodeLiteralDouble[S <: Double: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(lit: Double)) =>
        q"""
          _root_.io.circe.Decoder.instance[$sType] { c =>
            if (c.value.asNumber.map(_.toDouble).exists(_ == $lit)) {
              _root_.scala.util.Right[_root_.io.circe.DecodingFailure, $sType]($lit: $sType)
            } else {
              _root_.scala.util.Left(
                _root_.io.circe.DecodingFailure(
                  "Couldn't decode '" + $lit + "' double literal", c.history)
              )
            }
          }
        """
    }

  final def decodeLiteralFloat[S <: Float: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(lit: Float)) =>
        q"""
          _root_.io.circe.Decoder.instance[$sType] { c =>
            if (c.value.asNumber.map(_.toDouble).exists(s => s.toFloat == $lit)) {
              _root_.scala.util.Right[_root_.io.circe.DecodingFailure, $sType]($lit: $sType)
            } else {
              _root_.scala.util.Left(
                _root_.io.circe.DecodingFailure(
                  "Couldn't decode '" + $lit + "' float literal", c.history)
              )
            }
          }
        """
    }

  final def decodeLiteralLong[S <: Long: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(lit: Long)) =>
        q"""
          _root_.io.circe.Decoder.instance[$sType] { c =>
            if (c.value.asNumber.flatMap(_.toLong).exists(_ == $lit)) {
              _root_.scala.util.Right[_root_.io.circe.DecodingFailure, $sType]($lit: $sType)
            } else {
              _root_.scala.util.Left(
                _root_.io.circe.DecodingFailure(
                  "Couldn't decode '" + $lit + "' long literal", c.history)
              )
            }
          }
        """
    }

  final def decodeLiteralInt[S <: Int: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(lit: Int)) =>
        q"""
          _root_.io.circe.Decoder.instance[$sType] { c =>
            if (c.value.asNumber.flatMap(_.toInt).exists(_ == $lit)) {
              _root_.scala.util.Right[_root_.io.circe.DecodingFailure, $sType]($lit: $sType)
            } else {
              _root_.scala.util.Left(
                _root_.io.circe.DecodingFailure(
                  "Couldn't decode '" + $lit + "' int literal", c.history)
              )
            }
          }
        """
    }

  final def decodeLiteralChar[S <: Char: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(lit: Char)) =>
        q"""
          _root_.io.circe.Decoder.instance[$sType] { c =>
            if (c.value.asString.exists(s => s.length == 1 && s.charAt(0) == $lit)) {
              _root_.scala.util.Right[_root_.io.circe.DecodingFailure, $sType]($lit: $sType)
            } else {
              _root_.scala.util.Left(
                _root_.io.circe.DecodingFailure(
                  "Couldn't decode '" + $lit + "' char literal", c.history)
              )
            }
          }
        """
    }

  final def decodeLiteralBoolean[S <: Boolean: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(lit: Boolean)) =>
        q"""
          _root_.io.circe.Decoder.instance[$sType] { c =>
            if (c.value.asBoolean.exists(_ == $lit)) {
              _root_.scala.util.Right[_root_.io.circe.DecodingFailure, $sType]($lit: $sType)
            } else {
              _root_.scala.util.Left(
                _root_.io.circe.DecodingFailure(
                  "Couldn't decode '" + $lit + "' boolean literal", c.history)
              )
            }
          }
        """
    }

  final def encodeLiteralString[S <: String: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(_: String)) =>
        q"_root_.io.circe.Encoder.apply[_root_.java.lang.String].contramap[$sType](_root_.scala.Predef.identity)"
    }

  final def encodeLiteralDouble[S <: Double: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(_: Double)) =>
        q"_root_.io.circe.Encoder.apply[_root_.scala.Double].contramap[$sType](_root_.scala.Predef.identity)"
    }

  final def encodeLiteralFloat[S <: Float: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(_: Float)) =>
        q"_root_.io.circe.Encoder.apply[_root_.scala.Float].contramap[$sType](_root_.scala.Predef.identity)"
    }

  final def encodeLiteralLong[S <: Long: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(_: Long)) =>
        q"_root_.io.circe.Encoder.apply[_root_.scala.Long].contramap[$sType](_root_.scala.Predef.identity)"
    }

  final def encodeLiteralInt[S <: Int: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(_: Int)) =>
        q"_root_.io.circe.Encoder.apply[_root_.scala.Int].contramap[$sType](_root_.scala.Predef.identity)"
    }

  final def encodeLiteralChar[S <: Char: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(_: Char)) =>
        q"_root_.io.circe.Encoder.apply[_root_.scala.Char].contramap[$sType](_root_.scala.Predef.identity)"
    }

  final def encodeLiteralBoolean[S <: Boolean: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(_: Boolean)) =>
        q"_root_.io.circe.Encoder.apply[_root_.scala.Boolean].contramap[$sType](_root_.scala.Predef.identity)"
    }
}
