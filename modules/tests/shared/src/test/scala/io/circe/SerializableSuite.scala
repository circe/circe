package io.circe

import cats.laws.SerializableLaws
import cats.laws.discipline.SerializableTests
import io.circe.ast.{ Json, Printer }
import io.circe.tests.CirceSuite

class SerializableSuite extends CirceSuite {
  "Json" should "be serializable" in forAll { (j: Json) =>
    SerializableLaws.serializable(j); ()
  }

  "Cursor" should "be serializable" in forAll { (j: Json) =>
    SerializableLaws.serializable(Cursor(j)); ()
  }

  "HCursor" should "be serializable" in forAll { (j: Json) =>
    SerializableLaws.serializable(HCursor.fromJson(j)); ()
  }

  "ACursor" should "be serializable" in forAll { (j: Json) =>
    SerializableLaws.serializable(ACursor.ok(HCursor.fromJson(j))); ()
  }

  checkLaws("Decoder[Int]", SerializableTests.serializable(Decoder[Int]))
  checkLaws("Encoder[Int]", SerializableTests.serializable(Encoder[Int]))
  
  checkLaws(
  	"ObjectEncoder[Map[String, Int]]",
  	SerializableTests.serializable(ObjectEncoder[Map[String, Int]])
  )

  checkLaws("Parser", SerializableTests.serializable(parser.`package`))
  checkLaws("Printer", SerializableTests.serializable(Printer.noSpaces))
}
