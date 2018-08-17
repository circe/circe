package io.circe.jawn

import cats.data.Validated
import io.circe.Json
import io.circe.printer.Implicits._
import io.circe.testing.ParserTests
import io.circe.tests.CirceSuite
import io.circe.tests.examples.glossary
import java.io.File
import java.nio.ByteBuffer
import scala.io.Source

class JawnParserSuite extends CirceSuite {
  checkLaws("Parser", ParserTests(`package`).fromString)
  checkLaws("Parser", ParserTests(`package`)
    .fromFunction[ByteBuffer]("fromByteBuffer")(
      s => ByteBuffer.wrap(s.getBytes("UTF-8")),
      p => p.parseByteBuffer _,
      p => p.decodeByteBuffer[Json] _,
      p => p.decodeByteBufferAccumulating[Json] _
    )
  )

  "parse and decode(Accumulating)" should "fail on invalid input" in forAll { (s: String) =>
    assert(parse(s"Not JSON $s").isLeft)
    assert(decode[Json](s"Not JSON $s").isLeft)
    assert(decodeAccumulating[Json](s"Not JSON $s").isInvalid)
  }

  "parseFile and decodeFile(Accumulating)" should "parse a JSON file" in {
    val url = getClass.getResource("/io/circe/tests/examples/glossary.json")
    val file = new File(url.toURI)

    assert(decodeFile[Json](file) === Right(glossary))
    assert(decodeFileAccumulating[Json](file) == Validated.valid(glossary))
    assert(parseFile(file) === Right(glossary))
  }

  "parseByteBuffer and decodeByteBuffer(Accumulating)" should "parse a byte buffer" in {
    val stream = getClass.getResourceAsStream("/io/circe/tests/examples/glossary.json")
    val source = Source.fromInputStream(stream)
    val bytes = source.map(_.toByte).toArray
    source.close()

    assert(decodeByteBuffer[Json](ByteBuffer.wrap(bytes)) === Right(glossary))
    assert(decodeByteBufferAccumulating[Json](ByteBuffer.wrap(bytes)) == Validated.valid(glossary))
    assert(parseByteBuffer(ByteBuffer.wrap(bytes)) === Right(glossary))
  }
}
