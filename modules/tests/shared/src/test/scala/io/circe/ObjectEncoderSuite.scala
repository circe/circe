package io.circe

import cats.laws.discipline.ContravariantTests
import io.circe.syntax._
import io.circe.tests.CirceSuite

class ObjectEncoderSuite extends CirceSuite {
  checkLaws("ObjectEncoder[Int]", ContravariantTests[ObjectEncoder].contravariant[Int, Int, Int])

  "mapJsonObject" should "transform encoded output" in forAll { (m: Map[String, Int], k: String, v: Int) =>
    val newEncoder = ObjectEncoder[Map[String, Int]].mapJsonObject(_.add(k, v.asJson))

    assert(Decoder[Map[String, Int]].apply(newEncoder(m).hcursor) === Right(m.updated(k, v)))
  }
}
