package io.circe.jackson

import cats.data.Validated
import io.circe.Json
import io.circe.testing.ParserTests
import io.circe.tests.CirceSuite
import io.circe.tests.examples.glossary
import java.io.File
import scala.io.Source

class JacksonParserSuite extends CirceSuite with JacksonInstances {
  checkLaws("Parser", ParserTests(`package`).fromString(arbitraryCleanedJson))
  checkLaws("Parser", ParserTests(`package`)
    .fromFunction[Array[Byte]]("fromByteArray")(
      s => s.getBytes("UTF-8"),
      p => p.parseByteArray _,
      p => p.decodeByteArray[Json] _,
      p => p.decodeByteArrayAccumulating[Json] _
    )(arbitraryCleanedJson)
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

  "parseByteArray and decodeByteArray(Accumulating)" should "parse an array of bytes" in {
    val stream = getClass.getResourceAsStream("/io/circe/tests/examples/glossary.json")
    val source = Source.fromInputStream(stream)
    val bytes = source.map(_.toByte).toArray
    source.close()

    assert(decodeByteArray[Json](bytes) === Right(glossary))
    assert(decodeByteArrayAccumulating[Json](bytes) === Validated.valid(glossary))
    assert(parseByteArray(bytes) === Right(glossary))
  }
}
