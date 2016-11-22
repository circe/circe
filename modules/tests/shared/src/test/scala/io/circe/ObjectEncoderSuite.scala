package io.circe

import io.circe.syntax._
import io.circe.tests.CirceSuite

class ObjectEncoderSuite extends CirceSuite {
  "mapJsonObject" should "transform encoded output" in forAll { (m: Map[String, Int], k: String, v: Int) =>
    val newEncoder = ObjectEncoder[Map[String, Int]].mapJsonObject(_.add(k, v.asJson))

    assert(Decoder[Map[String, Int]].decodeJson(newEncoder(m)) === Right(m.updated(k, v)))
  }
}
