package io.circe

import cats.data.Xor
import io.circe.syntax._
import io.circe.tests.CirceSuite

class ObjectEncoderSuite extends CirceSuite {
  test("mapJsonObject") {
    check { (m: Map[String, Int], k: String, v: Int) =>
      val newEncoder = ObjectEncoder[Map[String, Int]].mapJsonObject(_.add(k, v.asJson))

      Decoder[Map[String, Int]].apply(newEncoder(m).hcursor) === Xor.right(m.updated(k, v))
    }
  }
}
