package io.circe

import cats.kernel.instances.either._
import cats.kernel.instances.int._
import cats.kernel.instances.tuple._
import cats.kernel.instances.unit._
import cats.laws.discipline.MonadErrorTests
import io.circe.tests.CirceSuite

class KeyDecoderSuite extends CirceSuite {
  checkAll("KeyDecoder[Int]", MonadErrorTests[KeyDecoder, Unit].monadError[Int, Int, Int])
}
