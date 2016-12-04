package io.circe

import cats.laws.discipline.ContravariantTests
import io.circe.tests.CirceSuite

class ArrayEncoderSuite extends CirceSuite {
  checkLaws("ArrayEncoder[Int]", ContravariantTests[ArrayEncoder].contravariant[Int, Int, Int])
}
