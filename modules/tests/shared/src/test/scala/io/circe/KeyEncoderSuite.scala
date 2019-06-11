package io.circe

import cats.laws.discipline.ContravariantTests
import io.circe.tests.CirceSuite

class KeyEncoderSuite extends CirceSuite {
  checkLaws("KeyEncoder[Int]", ContravariantTests[KeyEncoder].contravariant[Int, Int, Int])
}
