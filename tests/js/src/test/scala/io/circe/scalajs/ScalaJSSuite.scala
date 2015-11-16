package io.circe.scalajs

import io.circe.tests.CirceSuite
import io.circe.generic.auto._
import scala.scalajs.js
import scalajs.js.Dynamic.{literal => sjson}

case class Sample(name: String)

case class Sample2(name: js.UndefOr[String])

class ScalaJSSuite extends CirceSuite {

  test("should decode js.Object to Scala Class") {
    val in = sjson(name = "decode")
    val out = decodeJS[Sample](in)
    assert(out.isRight && out.getOrElse(null).name == "decode")
  }

  test("should encode Scala class to js.Object") {
    assert(Sample("encode").asJSAny.asInstanceOf[js.Dynamic].name.toString == "encode")
  }

  test("should handle js.UndefOr[A] in decoding js.Object to Scala Class") {
    val in = sjson(name = "undefdecode")
    val out = decodeJS[Sample2](in)
    val in2 = sjson(name = js.undefined)
    val out2 = decodeJS[Sample2](in2)
    assert(out.isRight && out.getOrElse(null).name.get == "undefdecode")
    assert(out2.isRight && !out2.getOrElse(null).name.isDefined)
  }

  test("should handle js.UndefOr[A] in encoding Scala class to js.Object") {
    assert(Sample2("encodeundefined").asJSAny.asInstanceOf[js.Dynamic].name.toString == "encodeundefined")
    assert(js.isUndefined(Sample2(js.undefined).asJSAny.asInstanceOf[js.Dynamic].name))
  }

}