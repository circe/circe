package io.circe.parse

import cats.data.{ NonEmptyList, Validated, Xor }
import io.circe.Json
import io.circe.test.{ CirceSuite, ParserTests }
import java.io.File
import java.nio.ByteBuffer
import org.scalacheck.Prop.forAll
import scala.io.Source

class ParserSuite extends CirceSuite {
  val glossary: Json = Json.obj(
    "glossary" -> Json.obj(
      "title" -> Json.string("example glossary"),
      "GlossDiv" -> Json.obj(
        "title" -> Json.string("S"),
        "GlossList" -> Json.obj(
          "GlossEntry" -> Json.obj(
            "ID" -> Json.string("SGML"),
            "SortAs" -> Json.string("SGML"),
            "GlossTerm" -> Json.string("Standard Generalized Markup Language"),
            "Acronym" -> Json.string("SGML"),
            "Abbrev" -> Json.string("ISO 8879:1986"),
            "GlossDef" -> Json.obj(
              "para" -> Json.string(
                "A meta-markup language, used to create markup languages such as DocBook."
              ),
              "GlossSeeAlso" -> Json.array(Json.string("GML"), Json.string("XML"))
            ),
            "GlossSee" -> Json.string("markup")
          )
        )
      )
    )
  )

  checkAll("Parser", ParserTests(`package`).parser)

  test("Parsing should fail on invalid input") {
    check {
      forAll { (s: String) =>
        parse(s"Not JSON $s").isLeft
      }
    }
  }
}
