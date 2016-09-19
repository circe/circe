package io.circe

import cats.data.Xor
import cats.laws.discipline.ContravariantTests
import io.circe.syntax._
import io.circe.tests.CirceSuite

class EncoderSuite extends CirceSuite {
  checkLaws("Encoder[Int]", ContravariantTests[Encoder].contravariant[Int, Int, Int])

  "mapJson" should "transform encoded output" in forAll { (m: Map[String, Int], k: String, v: Int) =>
    val newEncoder = Encoder[Map[String, Int]].mapJson(
      _.withObject(obj => Json.fromJsonObject(obj.add(k, v.asJson)))
    )

    assert(Decoder[Map[String, Int]].apply(newEncoder(m).hcursor) === Xor.right(m.updated(k, v)))
  }

  "Encoder[Enumeration]" should "write Scala Enumerations" in {
    object WeekDay extends Enumeration {
      type WeekDay = Value
      val Mon, Tue, Wed, Thu, Fri, Sat, Sun = Value
    }

    implicit val encoder = Encoder.enumEncoder(WeekDay)
    val json = WeekDay.Fri.asJson
    val decoder = Decoder.enumDecoder(WeekDay)
    assert(decoder.apply(json.hcursor) == Xor.right(WeekDay.Fri))
  }
}
