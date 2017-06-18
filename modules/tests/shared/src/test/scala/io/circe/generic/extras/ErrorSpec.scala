package io.circe
package generic.extras
package flex

import scala.util.{Try, Failure}
import scala.tools.reflect.{ToolBox, ToolBoxError}
import scala.reflect.runtime.universe

import tests.CirceSuite

class ErrorSpec
extends CirceSuite
{
  val cm = universe.runtimeMirror(getClass.getClassLoader)

  val toolbox = ToolBox(cm).mkToolBox()

  def compile(code: String) = toolbox.eval(toolbox.parse(code))

  def compileError(code: String) = {
    Try(compile(code)) match {
      case Failure(ToolBoxError(e, _)) => e.lines.toList.drop(2).mkString("\n")
      case a => sys.error(s"invalid error: $a")
    }
  }

  val rulesCode = """
  import io.circe.generic.extras.flex.all._

  case class T(num: Int, s: String)
  case class U(num: Int)
  class V(num: Int)
  {
    def n = num
  }
  case class W(t: T, u: U, v: V, w: U, x: V, y: V, z: U)

  def parseV(v: V) = U(v.n)

  derive[W](
    't.value[Int],
    'u \\ 'u,
    'v \\ 'values,
    'as \\ 'alues,
    'w \\ 'w >> parseV,
    'x \\ 'x >> parseV,
    'y.value[Int],
    'z.value[String]
  )
  """

  val rulesTarget = """deriving json decoder for `class W` failed because
 • you specified a map rule for 'x with result type `U`, but the field has type `V`
 • you used a `value` rule for field 'y which is not a case class
 • you specified a value rule for 't, but `T` has multiple fields
 • you specified an extraction rule for nonexistent field `as`
 • you must define or import a decoder of `V` for 'v (implicit Decoder[V] not found)
 • you must define or import a decoder of `V` for 'w (implicit Decoder[V] not found)
 • you specified type `String` for the value rule for 'z, but the field 'num of `U` is `Int`"""

  "deriving a decoder with erroneous overrides" should
    "give detailed explanations about why implicit resolution failed" in {
      assert(compileError(rulesCode) == rulesTarget)
    }
}
