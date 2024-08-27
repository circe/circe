/*
 * Copyright 2024 circe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.circe

import java.net.URI
import java.util.UUID

import io.circe.testing.KeyCodecTests
import io.circe.tests.CirceMunitSuite

class KeyCodecSuite extends CirceMunitSuite {

  checkAll("KeyCodec[String]", KeyCodecTests[String].keyCodec)
  checkAll("KeyCodec[Symbol]", KeyCodecTests[Symbol].keyCodec)
  checkAll("KeyCodec[UUID]", KeyCodecTests[UUID].keyCodec)
  checkAll("KeyCodec[URI]", KeyCodecTests[URI].keyCodec)
  checkAll("KeyCodec[Byte]", KeyCodecTests[Byte].keyCodec)
  checkAll("KeyCodec[Short]", KeyCodecTests[Short].keyCodec)
  checkAll("KeyCodec[Int]", KeyCodecTests[Int].keyCodec)
  checkAll("KeyCodec[Long]", KeyCodecTests[Long].keyCodec)
  checkAll("KeyCodec[Double]", KeyCodecTests[Double].keyCodec)

}
