package io.circe

import cats.laws.SerializableLaws
import cats.laws.discipline.SerializableTests
import io.circe.tests.CirceSuite

class SerializableSuite extends CirceSuite {
  test("Json") {
    check { (j: Json) => SerializableLaws.serializable(j) }
  }

  test("Cursor") {
    check { (j: Json) => SerializableLaws.serializable(j.cursor) }
  }

  test("HCursor") {
    check { (j: Json) => SerializableLaws.serializable(j.hcursor) }
  }

  test("ACursor") {
    check { (j: Json) => SerializableLaws.serializable(ACursor.ok(j.hcursor)) }
  }

  checkAll("Decoder[Int]", SerializableTests.serializable(Decoder[Int]))
  checkAll("Encoder[Int]", SerializableTests.serializable(Encoder[Int]))
  checkAll(
  	"ObjectEncoder[Map[String, Int]]",
  	SerializableTests.serializable(ObjectEncoder[Map[String, Int]])
  )

  checkAll("Parser", SerializableTests.serializable(parser.`package`))
  checkAll("Printer", SerializableTests.serializable(Printer.noSpaces))
}
