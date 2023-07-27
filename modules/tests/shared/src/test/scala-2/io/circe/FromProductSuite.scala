package io.circe

import syntax._
import io.circe.tests.CirceMunitSuite

class FromProductSuite extends CirceMunitSuite {

  test("encode correctly Example(id: Int, value: String)") {
    case class Example(id: Int, value: String)

    implicit val encoder: Encoder[Example] = Encoder.forProduct2("id", "value")(Example.unapply(_).get)
    implicit val decoder: Decoder[Example] = Decoder.forProduct2("id", "value")(Example.apply)

    val example = Example(1, "hello")

    val encoded = example.asJson
    val decoded = encoded.as[Example]

    assertEquals(decoded, Right(example))
  }

  test("encode correctly Example(id: Int, value: String) tupled") {
    case class Example(id: Int, value: String)

    implicit val encoder: Encoder[Example] = Encoder.forTupledProduct2("id", "value")(Example.unapply(_).get)
    implicit val decoder: Decoder[Example] = Decoder.forProduct2("id", "value")(Example.apply)

    val example = Example(1, "hello")

    val encoded = example.asJson
    val decoded = encoded.as[Example]

    assertEquals(decoded, Right(example))
  }

  test("codec encode/decode correctly Example(id: Int, value: String)") {
    case class Example(id: Int, value: String)

    implicit val encoder: Codec[Example] = Codec.forProduct2("id", "value")(Example.apply)(Example.unapply(_).get)

    val example = Example(1, "hello")

    val encoded = example.asJson
    val decoded = encoded.as[Example]

    assertEquals(decoded, Right(example))
  }

  test("codec encode/decode correctly Example(id: Int, value: String) tupled") {
    case class Example(id: Int, value: String)

    implicit val encoder: Codec[Example] = Codec.forTupledProduct2("id", "value")(Example.apply)(Example.unapply(_).get)

    val example = Example(1, "hello")

    val encoded = example.asJson
    val decoded = encoded.as[Example]

    assertEquals(decoded, Right(example))
  }
}
