package io.circe.scalajs

import algebra.Eq
import cats.data.Xor
import io.circe.generic.auto._
import io.circe.tests.CirceSuite
import scala.scalajs.js
import scalajs.js.Dynamic

case class Example(name: String)
case class UndefOrExample(name: js.UndefOr[String])

class ScalaJsSuite extends CirceSuite {
  implicit val eqThrowable: Eq[Throwable] = Eq.fromUniversalEquals

  test("should decode js.Object") {
    check { (s: String) =>
      decodeJs[Example](Dynamic.literal(name = s)).map(_.name) === Xor.right(s)
    }
  }

  test("should encode to js.Object") {
    check { (s: String) =>
      Example(s).asJsAny.asInstanceOf[js.Dynamic].name.toString === s
    }
  }

  test("should handle undefined js.UndefOr when decoding js.Object") {
    val res = decodeJs[UndefOrExample](Dynamic.literal(name = js.undefined))

    assert(res.map(_.name.isDefined) === Xor.right(false))
  }

  test("should handle defined js.UndefOr when decoding js.Object") {
    check { (s: String) =>
      decodeJs[UndefOrExample](Dynamic.literal(name = s)).map(_.name.get) === Xor.right(s)
    }
  }

  test("should handle defined js.UndefOr when encoding to js.Object") {
    check { (s: String) =>
      UndefOrExample(s).asJsAny.asInstanceOf[js.Dynamic].name.toString === s
    }
  }

  test("should handle undefined js.UndefOr when encoding to js.Object") {
    assert(js.isUndefined(UndefOrExample(js.undefined).asJsAny.asInstanceOf[js.Dynamic].name))
  }
}
