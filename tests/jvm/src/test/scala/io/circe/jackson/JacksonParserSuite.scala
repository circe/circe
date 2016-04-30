package io.circe.jackson

import cats.data.Xor
import io.circe.tests.{ CirceSuite, ParserTests }
import io.circe.tests.examples.glossary
import java.io.File
import scala.io.Source

class JacksonParserSuite extends CirceSuite {
  checkLaws("Parser", ParserTests(`package`).parser)

  "parse" should "fail on invalid input" in forAll { (s: String) =>
    assert(parse(s"Not JSON $s").isLeft)
  }

  "parseFile" should "parse a JSON file" in {
    val url = getClass.getResource("/io/circe/tests/examples/glossary.json")
    val file = new File(url.toURI)

    assert(parseFile(file) === Xor.right(glossary))
  }

  "parseByteArray" should "parse an array of bytes" in {
    val stream = getClass.getResourceAsStream("/io/circe/tests/examples/glossary.json")
    val source = Source.fromInputStream(stream)
    val bytes = source.map(_.toByte).toArray
    source.close()

    assert(parseByteArray(bytes) === Xor.right(glossary))
  }
}
