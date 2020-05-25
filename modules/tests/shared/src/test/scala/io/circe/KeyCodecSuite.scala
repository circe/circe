package io.circe

import java.util.UUID

import cats.instances.all._
import io.circe.testing.KeyCodecTests
import io.circe.tests.CirceSuite

class KeyCodecSuite extends CirceSuite {

  checkAll("KeyCodec[String]", KeyCodecTests[String].keyCodec)
  checkAll("KeyCodec[Symbol]", KeyCodecTests[Symbol].keyCodec)
  checkAll("KeyCodec[UUID]", KeyCodecTests[UUID].keyCodec)
  checkAll("KeyCodec[Byte]", KeyCodecTests[Byte].keyCodec)
  checkAll("KeyCodec[Short]", KeyCodecTests[Short].keyCodec)
  checkAll("KeyCodec[Int]", KeyCodecTests[Int].keyCodec)
  checkAll("KeyCodec[Long]", KeyCodecTests[Long].keyCodec)

}
