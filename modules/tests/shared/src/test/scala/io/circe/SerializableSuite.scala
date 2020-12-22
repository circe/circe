package io.circe

import cats.laws.discipline.SerializableTests
import cats.kernel.laws.SerializableLaws
import io.circe.tests.CirceMunitSuite
import org.scalacheck.Prop

class SerializableSuite extends CirceMunitSuite {
  property("Json should be serializable") {
    Prop.forAll { (j: Json) =>
      SerializableLaws.serializable(j); ()
    }
  }

  property("HCursor should be serializable") {
    Prop.forAll { (j: Json) =>
      SerializableLaws.serializable(j.hcursor); ()
    }
  }

  checkAll("Decoder[Int]", SerializableTests.serializable(Decoder[Int]))
  checkAll("Encoder[Int]", SerializableTests.serializable(Encoder[Int]))

  checkAll(
    "Encoder.AsArray[List[String]]",
    SerializableTests.serializable(Encoder.AsArray[List[String]])
  )

  checkAll(
    "Encoder.AsObject[Map[String, Int]]",
    SerializableTests.serializable(Encoder.AsObject[Map[String, Int]])
  )

  checkAll("Parser", SerializableTests.serializable(parser.`package`))
  checkAll("Printer", SerializableTests.serializable(Printer.noSpaces))
}
