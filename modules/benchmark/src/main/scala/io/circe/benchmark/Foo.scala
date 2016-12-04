package io.circe.benchmark

import cats.kernel.Eq

case class Foo(s: String, d: Double, i: Int, l: Long, bs: List[Boolean])

trait ArgonautFooInstances {
  import argonaut._, Argonaut._

  implicit val argonautCodecFoo: CodecJson[Foo] = CodecJson(
    {
      case Foo(s, d, i, l, bs) =>
        ("s" := s) ->: ("d" := d) ->: ("i" := i) ->: ("l" := l) ->: ("bs" := bs) ->: jEmptyObject
    },
    c => for {
      s  <- (c --\ "s").as[String]
      d  <- (c --\ "d").as[Double]
      i  <- (c --\ "i").as[Int]
      l  <- (c --\ "l").as[Long]
      bs <- (c --\ "bs").as[List[Boolean]]
    } yield Foo(s, d, i, l, bs)
  )
}

trait CirceFooInstances {
  import io.circe._, io.circe.syntax._

  implicit val circeEncodeFoo: Encoder[Foo] = new Encoder[Foo] {
    def apply(foo: Foo): Json = Json.obj(
      "s" -> foo.s.asJson,
      "d" -> foo.d.asJson,
      "i" -> foo.i.asJson,
      "l" -> foo.l.asJson,
      "bs" -> foo.bs.asJson
    )
  }

  implicit val circeDecodeFoo: Decoder[Foo] = new Decoder[Foo] {
    def apply(c: HCursor): Decoder.Result[Foo] = for {
      s <- c.get[String]("s").right
      d <- c.get[Double]("d").right
      i <- c.get[Int]("i").right
      l <- c.get[Long]("l").right
      bs <- c.get[List[Boolean]]("bs").right
    } yield Foo(s, d, i, l, bs)
  }
}

trait PlayFooInstances {
  import play.api.libs.functional.syntax._
  import play.api.libs.json.{ JsPath, Format }

  implicit val playFormatFoo: Format[Foo] = (
    (JsPath \ "s").format[String] and
    (JsPath \ "d").format[Double] and
    (JsPath \ "i").format[Int] and
    (JsPath \ "l").format[Long] and
    (JsPath \ "bs").format[List[Boolean]]
  )(Foo.apply, unlift(Foo.unapply))
}

trait SprayFooInstances {
  import spray.json._
  import spray.json.DefaultJsonProtocol._

  implicit val sprayFormatFoo: RootJsonFormat[Foo] = new RootJsonFormat[Foo] {
    def write(foo: Foo): JsObject = JsObject(
      "s" -> foo.s.toJson,
      "d" -> foo.d.toJson,
      "i" -> foo.i.toJson,
      "l" -> foo.l.toJson,
      "bs" -> foo.bs.toJson
    )

    def read(value: JsValue): Foo = value.asJsObject.getFields("s", "d", "i", "l", "bs") match {
      case Seq(JsString(s), d, i, l, bs) =>
        Foo(s, d.convertTo[Double], i.convertTo[Int], l.convertTo[Long], bs.convertTo[List[Boolean]])
      case _ => throw new DeserializationException("Foo expected")
    }
  }
}

object Foo extends ArgonautFooInstances with CirceFooInstances with PlayFooInstances with SprayFooInstances {
  implicit val eqFoo: Eq[Foo] = Eq.fromUniversalEquals[Foo]
}
