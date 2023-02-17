package io.circe

import syntax.*
import io.circe.tests.CirceMunitSuite

class FromProductSuite extends CirceMunitSuite:

    test("encode correctly Example(id: Int, value: String)") {
        case class Example(id: Int, value: String)

        given Encoder[Example] = Encoder.forProduct2("id", "value")(Tuple.fromProductTyped)
        given Decoder[Example] = Decoder.forProduct2("id", "value")(Example.apply)

        val example = Example(1, "hello")

        val encoded = example.asJson
        val decoded = encoded.as[Example]

        assertEquals(decoded, Right(example))
    }

    test("codec encode/decode correctly Example(id: Int, value: String)") {
        case class Example(id: Int, value: String)

        given Codec[Example] = Codec.forProduct2("id", "value")(Example.apply)(Tuple.fromProductTyped)

        val example = Example(1, "hello")

        val encoded = example.asJson
        val decoded = encoded.as[Example]

        assertEquals(decoded, Right(example))
    }
