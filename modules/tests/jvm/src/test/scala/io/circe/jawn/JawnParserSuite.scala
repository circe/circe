package io.circe.jawn

import cats.data.Xor
import io.circe.tests.{ CirceSuite, ParserTests }
import io.circe.tests.examples.glossary
import java.io.File
import java.nio.ByteBuffer
import scala.io.Source

class JawnParserSuite extends CirceSuite {
  checkLaws("Parser", ParserTests(`package`).parser)

  "parse" should "fail on invalid input" in forAll { (s: String) =>
    assert(parse(s"Not JSON $s").isLeft)
  }

  "parseFile" should "parse a JSON file" in {
    val url = getClass.getResource("/io/circe/tests/examples/glossary.json")
    val file = new File(url.toURI)

    assert(parseFile(file) === Xor.right(glossary))
  }

  "parseByteBuffer" should "parse a byte buffer" in {
    val stream = getClass.getResourceAsStream("/io/circe/tests/examples/glossary.json")
    val source = Source.fromInputStream(stream)
    val bytes = source.map(_.toByte).toArray
    source.close()

    val buffer = ByteBuffer.wrap(bytes)

    assert(parseByteBuffer(buffer) === Xor.right(glossary))
  }
}
