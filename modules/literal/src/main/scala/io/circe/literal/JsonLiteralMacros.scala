package io.circe.literal

import io.circe.Json
import java.lang.reflect.{ InvocationHandler, Method, Proxy }
import java.util.UUID
import macrocompat.bundle
import scala.Predef.classOf
import scala.reflect.macros.blackbox
import scala.util.control.NonFatal

@bundle
class JsonLiteralMacros(val c: blackbox.Context) {
  import c.universe._

  /**
   * Represents an interpolated expression that we've replaced with a unique string during parsing.
   */
  private[this] class Replacement(val placeholder: String, argument: Tree) {
    private[this] val argumentType = c.typecheck(argument).tpe

    def asJson: Tree = q"_root_.io.circe.Encoder[${ argumentType.widen }].apply($argument)"
    def asKey: Tree = q"_root_.io.circe.KeyEncoder[${ argumentType.widen }].apply($argument)"
  }

  private[this] object Replacement {
    private[this] final def generatePlaceholder(): String = UUID.randomUUID().toString

    def apply(stringParts: Seq[String], argument: Tree): Replacement = {
      /**
       * Generate a unique string that doesn't appear in the JSON literal.
       */
      val placeholder = Stream.continually(generatePlaceholder()).distinct.dropWhile(s =>
        stringParts.exists(_.contains(s))
      ).head

      new Replacement(placeholder, argument)
    }
  }

  private[this] abstract class Handler(replacements: Seq[Replacement]) extends InvocationHandler {
    protected[this] def invokeWithoutArgs: String => Object
    protected[this] def invokeWithArgs: (String, Array[Class[_]], Array[Object]) => Object

    final def invoke(proxy: Object, method: Method, args: Array[Object]): Object = {
      if (args.eq(null)) {
        invokeWithoutArgs(method.getName)
      } else {
        invokeWithArgs(method.getName, method.getParameterTypes, args)
      }
    }
    protected[this] final def toJsonKey(s: String): Tree =
      replacements.find(_.placeholder == s).fold(q"$s")(_.asKey)

    protected[this] final def toJsonString(s: CharSequence): Tree =
      replacements.find(_.placeholder == s).fold(q"_root_.io.circe.Json.fromString(${ s.toString })")(_.asJson)

    final def asProxy(cls: Class[_]): Object = Proxy.newProxyInstance(getClass.getClassLoader, Array(cls), this)
  }

  private[this] class SingleContextHandler(replacements: Seq[Replacement]) extends Handler(replacements) {
    private[this] var value: Tree = null

    protected[this] val invokeWithoutArgs: String => Object = {
      case "finish" => value
      case "isObj" => java.lang.Boolean.FALSE
    }

    protected[this] val invokeWithArgs: (String, Array[Class[_]], Array[Object]) => Object = {
      case ("add", Array(cls), Array(arg: CharSequence)) if cls == classOf[CharSequence] =>
        value = toJsonString(arg.toString)
        null
      case ("add", Array(_), Array(arg: Tree)) =>
        value = arg
        null
    }
  }

  private[this] class ArrayContextHandler(replacements: Seq[Replacement]) extends Handler(replacements) {
    private[this] var values: List[Tree] = Nil

    protected[this] val invokeWithoutArgs: String => Object = {
      case "finish" => q"_root_.io.circe.Json.arr(..$values)"
      case "isObj" => java.lang.Boolean.FALSE
    }

    protected[this] val invokeWithArgs: (String, Array[Class[_]], Array[Object]) => Object = {
      case ("add", Array(cls), Array(arg: CharSequence)) if cls == classOf[CharSequence] =>
        values = values :+ toJsonString(arg.toString)
        null
      case ("add", Array(_), Array(arg: Tree)) =>
        values = values :+ arg
        null
    }
  }

  private[this] class ObjectContextHandler(replacements: Seq[Replacement]) extends Handler(replacements) {
    private[this] var key: String = null
    private[this] var fields: List[Tree] = Nil

    protected[this] val invokeWithoutArgs: String => Object = {
      case "finish" => q"_root_.io.circe.Json.obj(..$fields)"
      case "isObj" => java.lang.Boolean.TRUE
    }

    protected[this] val invokeWithArgs: (String, Array[Class[_]], Array[Object]) => Object = {
      case ("add", Array(cls), Array(arg: CharSequence)) if cls == classOf[CharSequence] =>
        if (key.eq(null)) {
          key = arg.toString
        } else {
          fields = fields :+ q"(${ toJsonKey(key) }, ${ toJsonString(arg) })"
          key = null
        }
        null
      case ("add", Array(_), Array(arg: Tree)) =>
        fields = fields :+ q"(${ toJsonKey(key) }, $arg)"
        key = null
        null
    }
  }

  private[this] def jawnFContextClass = Class.forName("jawn.FContext")
  private[this] def jawnParserClass = Class.forName("jawn.Parser$")
  private[this] def jawnParser = jawnParserClass.getField("MODULE$").get(jawnParserClass)
  private[this] def jawnFacadeClass = Class.forName("jawn.Facade")
  private[this] def parseMethod = jawnParserClass.getMethod("parseUnsafe", classOf[String], jawnFacadeClass)

  private[this] class TreeFacadeHandler(replacements: Seq[Replacement]) extends Handler(replacements) {
    protected[this] val invokeWithoutArgs: String => Object = {
      case "jnull" => q"_root_.io.circe.Json.Null"
      case "jfalse" => q"_root_.io.circe.Json.False"
      case "jtrue" => q"_root_.io.circe.Json.True"
      case "singleContext" => new SingleContextHandler(replacements).asProxy(jawnFContextClass)
      case "arrayContext" => new ArrayContextHandler(replacements).asProxy(jawnFContextClass)
      case "objectContext" => new ObjectContextHandler(replacements).asProxy(jawnFContextClass)
    }

    protected[this] val invokeWithArgs: (String, Array[Class[_]], Array[Object]) => Object = {
      case ("jstring", Array(cls), Array(arg: CharSequence)) if cls == classOf[CharSequence] => toJsonString(arg)
      case ("jnum", Array(clsS, clsDecIndex, clsExpIndex), Array(s: CharSequence, decIndex, expIndex))
        if clsS == classOf[CharSequence] && clsDecIndex == classOf[Int] && clsExpIndex == classOf[Int] =>
          q"_root_.io.circe.Json.fromJsonNumber(_root_.io.circe.JsonNumber.fromString(${ s.toString }).get)"
    }
  }

  private[this] final def parse(jsonString: String, replacements: Seq[Replacement]): Either[Throwable, Tree] =
    try Right(
      parseMethod.invoke(
        jawnParser,
        jsonString,
        new TreeFacadeHandler(replacements).asProxy(jawnFacadeClass)
      ).asInstanceOf[Tree]
    ) catch {
      case NonFatal(e) => Left(e)
    }

  /**
   * Using `Tree` here instead of `c.Expr` fails to compile on 2.10.
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

      val replacements: Seq[Replacement] = args.map(argument => Replacement(stringParts, argument.tree))

      if (stringParts.size != replacements.size + 1) c.abort(
        c.enclosingPosition,
        "Invalid arguments for the json interpolator"
      ) else {
        val jsonString = stringParts.zip(replacements.map(_.placeholder)).foldLeft("") {
          case (acc, (part, placeholder)) =>
            val qm = "\""

            s"$acc$part$qm$placeholder$qm"
        } + stringParts.last

        c.Expr[Json](
          parse(jsonString, replacements) match {
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
}
