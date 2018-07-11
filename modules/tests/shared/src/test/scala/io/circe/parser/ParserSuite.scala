package io.circe.parser

import cats.data.Validated.Invalid
import cats.syntax.apply._
import cats.syntax.either._
import io.circe.{AccumulatingDecoder, Json}
import io.circe.testing.ParserTests
import io.circe.tests.CirceSuite

class ParserSuite extends CirceSuite {
  checkLaws("Parser", ParserTests(`package`).fromString)

  "parse and decode(Accumulating)" should "fail on invalid input" in forAll { (s: String) =>
    assert(parse(s"Not JSON $s").isLeft)
    assert(decode[Json](s"Not JSON $s").isLeft)
    assert(decodeAccumulating[Json](s"Not JSON $s").isInvalid)
  }

  "decodeAccumulating" should "returns all errors on invalid input" in {
    case class Sample(a: String, b: String)
    implicit val decoder: AccumulatingDecoder[Sample] = AccumulatingDecoder.instance {
      cursor => (
        cursor.get[String]("a").toValidatedNel,
        cursor.get[String]("b").toValidatedNel
      ).mapN(Sample.apply)
    }
    decodeAccumulating[Sample]("{}") match {
      case Invalid(errs) => assert(errs.length == 2)
      case _ => fail("Parser#decodeAccumulating returns Valid on invalid input")
    }
  }
}
