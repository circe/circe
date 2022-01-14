package io.circe

import cats.laws.discipline.ContravariantTests
import io.circe.tests.CirceMunitSuite

class KeyEncoderSuite extends CirceMunitSuite {
  checkAll("KeyEncoder[Int]", ContravariantTests[KeyEncoder].contravariant[Int, Int, Int])
}
