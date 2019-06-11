package io.circe

import cats.laws.discipline.SerializableTests
import cats.kernel.laws.SerializableLaws
import io.circe.tests.CirceSuite

class SerializableSuite extends CirceSuite {
  "Json" should "be serializable" in forAll { (j: Json) =>
    SerializableLaws.serializable(j); ()
  }

  "HCursor" should "be serializable" in forAll { (j: Json) =>
    SerializableLaws.serializable(j.hcursor); ()
  }

  checkLaws("Decoder[Int]", SerializableTests.serializable(Decoder[Int]))
  checkLaws("Encoder[Int]", SerializableTests.serializable(Encoder[Int]))

  checkLaws(
    "Encoder.AsArray[List[String]]",
    SerializableTests.serializable(Encoder.AsArray[List[String]])
  )

  checkLaws(
    "Encoder.AsObject[Map[String, Int]]",
    SerializableTests.serializable(Encoder.AsObject[Map[String, Int]])
  )

  checkLaws("Parser", SerializableTests.serializable(parser.`package`))
  checkLaws("Printer", SerializableTests.serializable(Printer.noSpaces))
}
