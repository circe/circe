package io.circe.parse

import cats.data.{ NonEmptyList, Validated, Xor }
import io.circe.Json
import io.circe.tests.{ CirceSuite, ParserTests }
import java.io.File
import java.nio.ByteBuffer
import org.scalacheck.Prop.forAll
import scala.io.Source

class ParserSuite extends CirceSuite {
  checkAll("Parser", ParserTests(`package`).parser)

  test("Parsing should fail on invalid input") {
    check {
      forAll { (s: String) =>
        parse(s"Not JSON $s").isLeft
      }
    }
  }
}
