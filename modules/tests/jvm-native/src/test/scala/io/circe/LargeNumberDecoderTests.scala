/*
 * Copyright 2023 circe
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

import io.circe.parser.parse
import io.circe.tests.CirceMunitSuite
import org.scalacheck.Prop._

/**
 * Tests that fail because of bugs (or at least limitations) on Scala.js.
 */
trait LargeNumberDecoderTests { this: CirceMunitSuite =>
  property("Decoder[Long] should succeed on whole decimal values (#83)") {
    forAll { (v: Long, n: Byte) =>
      val zeros = "0" * (math.abs(n.toInt) + 1)
      val Right(json) = parse(s"$v.$zeros")
      Decoder[Long].apply(json.hcursor) ?= Right(v)
    }
  }

  property("Decoder[BigInt] should succeed on whole decimal values (#83)") {
    forAll { (v: BigInt, n: Byte) =>
      val zeros = "0" * (math.abs(n.toInt) + 1)
      val Right(json) = parse(s"$v.$zeros")
      Decoder[BigInt].apply(json.hcursor) ?= Right(v)
    }
  }
}

trait LargeNumberDecoderTestsMunit { this: CirceMunitSuite =>
  property("Decoder[Long] should succeed on whole decimal values (#83)") {
    forAll { (v: Long, n: Byte) =>
      val zeros = "0" * (math.abs(n.toInt) + 1)
      val Right(json) = parse(s"$v.$zeros")
      Decoder[Long].apply(json.hcursor) ?= Right(v)
    }
  }

  property("Decoder[BigInt] should succeed on whole decimal values (#83)") {
    forAll { (v: BigInt, n: Byte) =>
      val zeros = "0" * (math.abs(n.toInt) + 1)
      val Right(json) = parse(s"$v.$zeros")
      Decoder[BigInt].apply(json.hcursor) ?= Right(v)
    }
  }
}
