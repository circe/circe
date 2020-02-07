package io.circe.scalajs

import cats.instances.all._
import cats.syntax.eq._
import io.circe.{ Decoder, Encoder, Json }
import io.circe.syntax._
import io.circe.tests.CirceSuite
import scala.scalajs.js
import scalajs.js.Dynamic

case class Example(name: String)

object Example {
  implicit val decodeExample: Decoder[Example] = Decoder.forProduct1("name")(Example.apply)
  implicit val encodeExample: Encoder[Example] = Encoder.forProduct1("name") {
    case Example(name) => Tuple1(name)
  }
}

case class UndefOrExample(name: js.UndefOr[String])

object UndefOrExample {
  implicit val decodeUndefOrExample: Decoder[UndefOrExample] = Decoder.forProduct1("name")(UndefOrExample.apply)
  implicit val encodeUndefOrExample: Encoder[UndefOrExample] = Encoder.instance {
    case UndefOrExample(name) => Json.obj("name" -> name.asJson)
  }
}

class ScalaJsSuite extends CirceSuite {
  "decodeJs" should "decode js.Object" in forAll { (s: String) =>
    assert(decodeJs[Example](Dynamic.literal(name = s)).map(_.name) === Right(s))
  }

  it should "handle undefined js.UndefOr when decoding js.Object" in {
    val res = decodeJs[UndefOrExample](Dynamic.literal(name = js.undefined))

    assert(res.map(_.name.isDefined) === Right(false))
  }

  it should "handle defined js.UndefOr when decoding js.Object" in forAll { (s: String) =>
    assert(decodeJs[UndefOrExample](Dynamic.literal(name = s)).map(_.name.toOption.get) === Right(s))
  }

  "asJsAny" should "encode to js.Object" in forAll { (s: String) =>
    assert(Example(s).asJsAny.asInstanceOf[js.Dynamic].name.toString === s)
  }

  it should "handle defined js.UndefOr when encoding to js.Object" in forAll { (s: String) =>
    assert(UndefOrExample(s).asJsAny.asInstanceOf[js.Dynamic].name.toString === s)
  }

  it should "handle undefined js.UndefOr when encoding to js.Object" in {
    assert(Option(UndefOrExample(js.undefined).asJsAny.asInstanceOf[js.Dynamic].name).isEmpty)
  }
}
