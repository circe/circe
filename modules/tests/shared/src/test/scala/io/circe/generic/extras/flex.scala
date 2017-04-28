package io.circe
package generic.extras
package flex

import cats.Functor

import parser.decode

import shapeless.HNil
import shapeless.syntax.singleton._

import flex.all._
import tests.CirceSuite

class AutoSuite extends CirceSuite {
  case class V(a: Int)

  implicit def dec = deriveH[V]('a ->> Auto[Int]() :: HNil)

  "specifying the `Auto` extractor" should "use the generic decoder" in forAll { (num: Int) =>
    assert(decode[V](s"""{"a": $num}""") == Right(V(num)))
  }
}

class ValueClassSuite extends CirceSuite {
  case class V(a: Int)
  case class Outer(value: V)

  "specifying the `Value` extractor" should "fetch a subfield" in forAll { (value: Int) =>
    implicit def dec = deriveH[Outer](
      ('value ->> Value[Path, Int](Path(List("value")))) :: HNil
    )
    assert(decode[Outer](s"""{"value": $value}""") == Right(Outer(V(value))))
  }

  "specifying a `Value` with nested `Ap`" should "transform data before applying the class" in forAll { (value: Int) =>
    implicit def dec =
      deriveH[Outer](
        ('value ->> Value[Ap[Path, String, Int], Int](Ap(Path(List("value")), (_: String).toInt))) :: HNil
      )
    assert(decode[Outer](s"""{"value": "$value"}""") == Right(Outer(V(value))))
  }
}

class ApSuite extends CirceSuite {
  case class V(nums: List[Int])

  val trans = (in: Option[List[Int]]) => in getOrElse Nil

  implicit def dec = deriveH[V](
    'nums ->> Ap[Path, Option[List[Int]], List[Int]](Path(List("nums")), trans) :: HNil
  )

  "specifying an `Ap` extractor" should "map an Option[List[A]] to a List[A]" in forAll { (nums: List[Int]) =>
    assert(decode[V](s"""{"nums": ${nums.mkString("[", ",", "]")}}""") == Right(V(nums)))
  }
}

class FmapSuite extends CirceSuite {
  case class V(num: Option[Int])

  val trans = (in: String) => in.toInt

  implicit def dec = deriveH[V](
    'num ->> Fmap[Path, String, Int](Path(List("num")), trans) :: HNil
  )

  "specifying an `Fmap` extractor" should "map over an Option" in forAll { (num: Int) =>
    assert(decode[V](s"""{"num": "$num"}""") == Right(V(Some(num))))
  }
}

class DefaultValueSuite
extends CirceSuite
{
  trait Y
  case class X(a: String, b: Boolean = false, private val c: Map[String, Y] = Map.empty)

  case class V(x: X)

  def trans(n: Int): X = X(n.toString)

  implicit def dec = deriveH[V](
    'x ->> Ap[Path, Int, X](Path(List("x")), trans) :: HNil
  )

  "decoding a class with default values" should "use the defaults for nonexisting fields" in forAll { (num: Int) =>
    assert(decode[V](s"""{"x": $num}""") == Right(V(X(num.toString))))
  }
}

class ManualChainSuite
extends CirceSuite
{
  case class X(num: Int)
  case class V(x: X)

  implicit def dec = deriveH[V](
    'x ->> Chain(Chain(Path(List("a")), Manual(c => c.downArray.right)), Path(List("x"))) :: HNil
  )

  "specify traversal manually, chained with `Path`s" should "concat the path elements" in forAll { (num: Int) =>
    assert(decode[V](s"""{"a": [{}, {"x": {"num": $num}}, {}]}""") == Right(V(X(num))))
  }
}

class DslSuite
extends CirceSuite
{
  case class V(num: Int)
  case class Inner(nums: List[V], default: String = "default")
  case class Outer(inner: Option[Inner], auto: List[V], value: V, absent: Option[String], man: V, mapval: V,
    fmapval: Option[Int], in: Int, sub: Int, mapFmap: List[Int], default: Int = 11, fmap2: List[Int])

  case class Value(value: String)

  def values[F[_]: Functor] = (vs: F[Value]) => vs map (_.value)

  val fix = """
  {
    "root": {
      "in": {
        "a": ["1", "2", "3"],
        "default": "override"
      },
      "nest": {
        "in": {
          "val": 31
        }
      }
    },
    "auto": [{"num": 4}],
    "value": 5,
    "custom": { "path": [1, 2, { "target": 3 }] },
    "valmap": "9",
    "sub": {
      "a": {
        "b": 19
      }
    },
    "fmapval": "13",
    "mapFmap": [{ "value": "27" }, { "value": "29" }],
    "fmap2": ["37", "41"]
  }
  """

  def parseInt(s: String): Int = s.toInt

  def parseInts(l: List[String]): List[V] = l.map(parseInt).map(V(_))

  implicit val innerDecoder = derive[Inner](
    'nums \\ 'a >> parseInts
  )

  implicit val outerDecoder = derive[Outer](
    'inner \\ 'root \ 'in,
    'value.value[Int],
    (('man \\ 'custom).fetch(_.downField("path").downArray.right.right) \ 'target).value[Int],
    ('mapval \\ 'valmap >> parseInt).value[Int],
    'default \\ 'foo >> parseInt,
    'in.in('root, 'nest) \ 'val,
    'sub \ 'a \ 'b,
    'fmapval >>> parseInt,
    'mapFmap >> values[List] >>> parseInt,
    'fmap2 >>> parseInt >>> ((_: Int) + 1)
  )

  "passing special rules to `derive`" should "create a decoder" in {
    assert(
      decode[Outer](fix) == Right(
        Outer(
          Some(Inner(List(V(1), V(2), V(3)), "override")),
          List(V(4)), V(5), None, V(3), V(9), Some(13), 31, 19, List(27, 29), 11, List(38, 42))
      )
    )
  }
}
