package io.circe.jawn

import cats.data.Xor
import io.circe.tests.{ CirceSuite, ParserTests }
import io.circe.tests.examples.glossary
import java.io.File
import java.nio.ByteBuffer
import scala.io.Source

class JawnParserSuite extends CirceSuite {
  checkAll("Parser", ParserTests(`package`).parser)

  test("Parsing should fail on invalid input") {
    check { (s: String) =>
      parse(s"Not JSON $s").isLeft
    }
  }

  test("parseFile") {
    val url = getClass.getResource("/io/circe/tests/examples/glossary.json")
    val file = new File(url.toURI)

    assert(parseFile(file) === Xor.right(glossary))
  }

  test("parseByteBuffer") {
    val stream = getClass.getResourceAsStream("/io/circe/tests/examples/glossary.json")
    val source = Source.fromInputStream(stream)
    val bytes = source.map(_.toByte).toArray
    source.close()

    val buffer = ByteBuffer.wrap(bytes)

    assert(parseByteBuffer(buffer) === Xor.right(glossary))
  }
}
