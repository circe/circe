package io.circe

import cats.data.Xor
import io.circe.syntax._
import io.circe.tests.CirceSuite

class EncoderSuite extends CirceSuite {
  test("mapJson") {
    check { (m: Map[String, Int], k: String, v: Int) =>
      val newEncoder = Encoder[Map[String, Int]].mapJson(
        _.withObject(obj => Json.fromJsonObject(obj.add(k, v.asJson)))
      )

      Decoder[Map[String, Int]].apply(newEncoder(m).hcursor) === Xor.right(m.updated(k, v))
    }
  }
}
