package io.circe

import cats.laws.SerializableLaws
import cats.laws.discipline.SerializableTests
import io.circe.export.Exported
import io.circe.tests.CirceSuite

class SerializableSuite extends CirceSuite {
  "Json" should "be serializable" in forAll { (j: Json) =>
    SerializableLaws.serializable(j); ()
  }

  "HCursor" should "be serializable" in forAll { (j: Json) =>
    SerializableLaws.serializable(j.hcursor); ()
  }

  "Exported" should "be serializable" in forAll { (value: String) =>
    SerializableLaws.serializable(Exported(value)); ()
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
