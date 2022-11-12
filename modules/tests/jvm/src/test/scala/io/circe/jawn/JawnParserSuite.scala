package io.circe.jawn

import cats.data.Validated
import cats.syntax.eq._
import io.circe.Json
import io.circe.testing.{ EqInstances, ParserTests }
import io.circe.tests.CirceMunitSuite
import io.circe.tests.examples.glossary
import java.io.File
import java.nio.ByteBuffer
import org.scalacheck.Prop._
import scala.io.Source

class JawnParserSuite extends CirceMunitSuite with EqInstances {
  checkAll("Parser", ParserTests(`package`).fromString)
  checkAll(
    "Parser",
    ParserTests(`package`).fromFunction[ByteBuffer]("fromByteBuffer")(
      s => ByteBuffer.wrap(s.getBytes("UTF-8")),
      p => p.parseByteBuffer _,
      p => p.decodeByteBuffer[Json] _,
      p => p.decodeByteBufferAccumulating[Json] _
    )
  )

  property("parse and decode(Accumulating) should fail on invalid input")(parseAccumulatingProp)

  private lazy val parseAccumulatingProp = forAll { (s: String) =>
    assert(parse(s"Not JSON $s").isLeft)
    assert(decode[Json](s"Not JSON $s").isLeft)
    assert(decodeAccumulating[Json](s"Not JSON $s").isInvalid)
  }

  test("parseFile and decodeFile(Accumulating) should parse a JSON file") {
    val url = getClass.getResource("/io/circe/tests/examples/glossary.json")
    val file = new File(url.toURI)

    assertEquals(decodeFile[Json](file), Right(glossary))
    assertEquals(decodeFileAccumulating[Json](file), Validated.valid(glossary))
    assertEquals(parseFile(file), Right(glossary))
  }

  test("parseByteBuffer and decodeByteBuffer(Accumulating) should parse a byte buffer") {
    val stream = getClass.getResourceAsStream("/io/circe/tests/examples/glossary.json")
    val source = Source.fromInputStream(stream)
    val bytes = source.map(_.toByte).toArray
    source.close()

    assertEquals(decodeByteBuffer[Json](ByteBuffer.wrap(bytes)), Right(glossary))
    assertEquals(decodeByteBufferAccumulating[Json](ByteBuffer.wrap(bytes)), Validated.valid(glossary))
    assertEquals(parseByteBuffer(ByteBuffer.wrap(bytes)), Right(glossary))
  }

  test("parseByteArray and decodeByteArray(Accumulating) should parse a byte array") {
    val stream = getClass.getResourceAsStream("/io/circe/tests/examples/glossary.json")
    val source = Source.fromInputStream(stream)
    val bytes = source.map(_.toByte).toArray
    source.close()

    assertEquals(decodeByteArray[Json](bytes), Right(glossary))
    assertEquals(decodeByteArrayAccumulating[Json](bytes), Validated.valid(glossary))
    assertEquals(parseByteArray(bytes), Right(glossary))
  }
}
