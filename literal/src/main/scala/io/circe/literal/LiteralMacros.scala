package io.circe.literal

import io.circe.Json
import java.lang.reflect.{ InvocationHandler, Method, Proxy }
import java.util.UUID
import macrocompat.bundle
import scala.reflect.macros.whitebox
import scala.util.control.NonFatal

@bundle
class LiteralMacros(val c: whitebox.Context) {
  import c.universe._

  private[this] abstract class HandlerHelpers(placeHolders: Map[String, (Tree, Option[Tree])])
    extends InvocationHandler {
    final def invoke(proxy: Object, method: Method, args: Array[Object]): Object =
      (args, method.getParameterTypes) match {
        case (Array(arg), Array(cls)) => invokeWithArg(method.getName, cls, arg)
        case _ => invokeWithoutArg(method.getName)
      }

    def invokeWithoutArg: String => Object
    def invokeWithArg: (String, Class[_], Object) => Object

    def toJsonKey(s: String): Tree = placeHolders.get(s).flatMap(_._2).getOrElse(q"$s")

    def toJsonString(s: String): Tree =
      placeHolders.get(s).map(_._1).getOrElse(q"_root_.io.circe.Json.fromString($s)")

    def asProxy(cls: Class[_]): Object =
      Proxy.newProxyInstance(getClass.getClassLoader, Array(cls), this)
  }

  private[this] class SingleContextHandler(placeHolders: Map[String, (Tree, Option[Tree])])
    extends HandlerHelpers(placeHolders) {
    var value: Tree = null

    val invokeWithoutArg: String => Object = {
      case "finish" => value
      case "isObj" => false: java.lang.Boolean
    }

    val invokeWithArg: (String, Class[_], Object) => Object = {
      case ("add", cls, arg: String) if cls == classOf[String] =>
        value = toJsonString(arg)
        null
      case ("add", cls, arg: Tree) =>
        value = arg
        null
    }
  }

  private[this] class ArrayContextHandler(placeHolders: Map[String, (Tree, Option[Tree])])
    extends HandlerHelpers(placeHolders) {
    var values: List[Tree] = Nil

    val invokeWithoutArg: String => Object = {
      case "finish" => q"_root_.io.circe.Json.arr(..$values)"
      case "isObj" => false: java.lang.Boolean
    }

    val invokeWithArg: (String, Class[_], Object) => Object = {
      case ("add", cls, arg: String) if cls == classOf[String] =>
        values = values :+ toJsonString(arg)
        null
      case ("add", cls, arg: Tree) =>
        values = values :+ arg
        null
    }
  }

  private[this] class ObjectContextHandler(placeHolders: Map[String, (Tree, Option[Tree])])
    extends HandlerHelpers(placeHolders) {
    var key: String = null
    var fields: List[Tree] = Nil

    val invokeWithoutArg: String => Object = {
      case "finish" => q"_root_.io.circe.Json.obj(..$fields)"
      case "isObj" => true: java.lang.Boolean
    }

    val invokeWithArg: (String, Class[_], Object) => Object = {
      case ("add", cls, arg: String) if cls == classOf[String] =>
        if (key == null) {
          key = arg
        } else {
          fields = fields :+ q"(${ toJsonKey(key) }, ${ toJsonString(arg) })"
          key = null
        }
        null
      case ("add", cls, arg: Tree) =>
        fields = fields :+ q"(${ toJsonKey(key) }, $arg)"
        key = null
        null
    }
  }

  private[this] class TreeFacadeHandler(placeHolders: Map[String, (Tree, Option[Tree])])
    extends HandlerHelpers(placeHolders) {
    val invokeWithoutArg: String => Object = {
      case "jnull" => q"_root_.io.circe.Json.Null"
      case "jfalse" => q"_root_.io.circe.Json.False"
      case "jtrue" => q"_root_.io.circe.Json.True"
      case "singleContext" =>
        new SingleContextHandler(placeHolders).asProxy(Class.forName("jawn.FContext"))
      case "arrayContext" =>
        new ArrayContextHandler(placeHolders).asProxy(Class.forName("jawn.FContext"))
      case "objectContext" =>
        new ObjectContextHandler(placeHolders).asProxy(Class.forName("jawn.FContext"))
    }

    val invokeWithArg: (String, Class[_], Object) => Object = {
      case ("jnum", cls, arg: String) if cls == classOf[String] => q"""
        _root_.io.circe.Json.fromJsonNumber(
          _root_.io.circe.JsonNumber.unsafeDecimal($arg)
        )
      """
      case ("jint", cls, arg: String) if cls == classOf[String] => q"""
        _root_.io.circe.Json.fromJsonNumber(
          _root_.io.circe.JsonNumber.unsafeIntegral($arg)
        )
      """
      case ("jstring", cls, arg: String) if cls == classOf[String] => toJsonString(arg)
    }
  }

  final def parse(
    jsonString: String,
    placeHolders: Map[String, (Tree, Option[Tree])]
  ): Either[Throwable, Tree] =
    try Right {
      val jawnParserClass = Class.forName("jawn.Parser$")
      val jawnParser = jawnParserClass.getField("MODULE$").get(jawnParserClass)
      val jawnFacadeClass = Class.forName("jawn.Facade")
      val parseMethod = jawnParserClass.getMethod("parseUnsafe", classOf[String], jawnFacadeClass)

      parseMethod.invoke(
        jawnParser,
        jsonString,
        new TreeFacadeHandler(placeHolders).asProxy(jawnFacadeClass)
      ).asInstanceOf[Tree]
    } catch {
      case NonFatal(e) => Left(e)
    }

  private[this] final def randomPlaceHolder(): String = UUID.randomUUID().toString

  /**
   * Using Tree here instead of c.Expr fails to compile on 2.10.
   */
  final def jsonStringContext(args: c.Expr[Any]*): c.Expr[Json] = c.prefix.tree match {
    case Apply(_, Apply(_, parts) :: Nil) =>
      val stringParts = parts.map {
        case Literal(Constant(part: String)) => part
        case _ => c.abort(
          c.enclosingPosition,
          "A StringContext part for the json interpolator is not a string"
        )
      }

      val encodedArgs: Seq[(String, (Tree, Option[Tree]))] = args.map { arg =>
        val tpe = c.typecheck(arg.tree).tpe
        val placeHolder = Stream.continually(randomPlaceHolder()).distinct.dropWhile(s =>
          stringParts.exists(_.contains(s))
        ).head

        (
          placeHolder,
          (
            q"_root_.io.circe.Encoder[$tpe].apply($arg)",
            if (tpe =:= typeOf[String]) Some(q"$arg") else None
          )
        )
      }

      val placeHolders = encodedArgs.map(_._1)

      if (stringParts.size != encodedArgs.size + 1) c.abort(
        c.enclosingPosition,
        "Invalid arguments for the json interpolator"
      ) else {
        val jsonString = stringParts.zip(placeHolders).foldLeft("") {
          case (acc, (part, placeHolder)) =>
            val qm = "\""

            s"$acc$part$qm$placeHolder$qm"
        } + stringParts.last

        c.Expr[Json](
          parse(jsonString, encodedArgs.toMap) match {
            case Right(tree) => tree
            case Left(_: ClassNotFoundException) => c.abort(
              c.enclosingPosition,
              "The json interpolator requires jawn to be available at compile time"
            )
            case Left(t: Throwable) => c.abort(
              c.enclosingPosition,
              "Invalid JSON in interpolated string"
            )
          }
        )
      }
    case _ => c.abort(c.enclosingPosition, "Invalid use of the json interpolator")
  }

  final def decodeLiteralStringImpl[S <: String: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(lit: String)) =>
        val name = s"""String("$lit")"""

        q"""
          _root_.io.circe.Decoder.instance[$sType] { c =>
            if (c.focus.asString.exists(_ == $lit)) {
              _root_.scala.util.Right[_root_.io.circe.DecodingFailure, $sType]($lit: $sType)
            } else {
              _root_.scala.util.Left(
                _root_.io.circe.DecodingFailure($name, c.history)
              )
            }
          }
        """
    }

  final def decodeLiteralBooleanImpl[S <: Boolean: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(lit: Boolean)) =>
        val name = s"""Boolean($lit)"""

        q"""
          _root_.io.circe.Decoder.instance[$sType] { c =>
            if (c.focus.asBoolean.exists(_ == $lit)) {
              _root_.scala.util.Right[_root_.io.circe.DecodingFailure, $sType]($lit: $sType)
            } else {
              _root_.scala.util.Left(
                _root_.io.circe.DecodingFailure($name, c.history)
              )
            }
          }
        """
    }

  final def decodeLiteralDoubleImpl[S <: Double: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(lit: Double)) =>
        val name = s"""Double($lit)"""

        q"""
          _root_.io.circe.Decoder.instance[$sType] { c =>
            if (c.focus.asNumber.map(_.toDouble).exists(_ == $lit)) {
              _root_.scala.util.Right[_root_.io.circe.DecodingFailure, $sType]($lit: $sType)
            } else {
              _root_.scala.util.Left(
                _root_.io.circe.DecodingFailure($name, c.history)
              )
            }
          }
        """
    }

  final def decodeLiteralFloatImpl[S <: Float: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(lit: Float)) =>
        val name = s"""Float($lit)"""

        q"""
          _root_.io.circe.Decoder.instance[$sType] { c =>
            if (c.focus.asNumber.map(_.toDouble).exists(s => s.toFloat == $lit)) {
              _root_.scala.util.Right[_root_.io.circe.DecodingFailure, $sType]($lit: $sType)
            } else {
              _root_.scala.util.Left(
                _root_.io.circe.DecodingFailure($name, c.history)
              )
            }
          }
        """
    }

  final def decodeLiteralLongImpl[S <: Long: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(lit: Long)) =>
        val name = s"""Long($lit)"""

        q"""
          _root_.io.circe.Decoder.instance[$sType] { c =>
            if (c.focus.asNumber.flatMap(_.toLong).exists(_ == $lit)) {
              _root_.scala.util.Right[_root_.io.circe.DecodingFailure, $sType]($lit: $sType)
            } else {
              _root_.scala.util.Left(
                _root_.io.circe.DecodingFailure($name, c.history)
              )
            }
          }
        """
    }

  final def decodeLiteralIntImpl[S <: Int: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(lit: Int)) =>
        val name = s"""Int($lit)"""

        q"""
          _root_.io.circe.Decoder.instance[$sType] { c =>
            if (c.focus.asNumber.flatMap(_.toInt).exists(_ == $lit)) {
              _root_.scala.util.Right[_root_.io.circe.DecodingFailure, $sType]($lit: $sType)
            } else {
              _root_.scala.util.Left(
                _root_.io.circe.DecodingFailure($name, c.history)
              )
            }
          }
        """
    }

  final def decodeLiteralCharImpl[S <: Char: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(lit: Char)) =>
        val name = s"""Char($lit)"""

        q"""
          _root_.io.circe.Decoder.instance[$sType] { c =>
            if (c.focus.asString.exists(s => s.length == 1 && s.charAt(0) == $lit)) {
              _root_.scala.util.Right[_root_.io.circe.DecodingFailure, $sType]($lit: $sType)
            } else {
              _root_.scala.util.Left(
                _root_.io.circe.DecodingFailure($name, c.history)
              )
            }
          }
        """
    }

  final def encodeLiteralStringImpl[S <: String: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(lit: String)) =>
        q"_root_.io.circe.Encoder.apply[_root_.java.lang.String].contramap[$sType](_root_.scala.Predef.identity)"
    }

  final def encodeLiteralBooleanImpl[S <: Boolean: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(lit: Boolean)) =>
        q"_root_.io.circe.Encoder.apply[_root_.scala.Boolean].contramap[$sType](_root_.scala.Predef.identity)"
    }

  final def encodeLiteralDoubleImpl[S <: Double: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(lit: Double)) =>
        q"_root_.io.circe.Encoder.apply[_root_.scala.Double].contramap[$sType](_root_.scala.Predef.identity)"
    }

  final def encodeLiteralFloatImpl[S <: Float: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(lit: Float)) =>
        q"_root_.io.circe.Encoder.apply[_root_.scala.Float].contramap[$sType](_root_.scala.Predef.identity)"
    }

  final def encodeLiteralLongImpl[S <: Long: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(lit: Long)) =>
        q"_root_.io.circe.Encoder.apply[_root_.scala.Long].contramap[$sType](_root_.scala.Predef.identity)"
    }

  final def encodeLiteralIntImpl[S <: Int: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(lit: Int)) =>
        q"_root_.io.circe.Encoder.apply[_root_.scala.Int].contramap[$sType](_root_.scala.Predef.identity)"
    }

  final def encodeLiteralCharImpl[S <: Char: c.WeakTypeTag]: Tree =
    weakTypeOf[S].dealias match {
      case sType @ ConstantType(Constant(lit: Char)) =>
        q"_root_.io.circe.Encoder.apply[_root_.scala.Char].contramap[$sType](_root_.scala.Predef.identity)"
    }
}
