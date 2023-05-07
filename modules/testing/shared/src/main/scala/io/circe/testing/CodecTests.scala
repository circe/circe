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

package io.circe.testing

import cats.instances.either._
import cats.kernel.Eq
import cats.kernel.laws.SerializableLaws
import cats.laws._
import cats.laws.discipline._
import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
import org.scalacheck.Arbitrary
import org.scalacheck.Prop
import org.scalacheck.Shrink
import org.typelevel.discipline.Laws

trait CodecLaws[A] {
  def decode: Decoder[A]
  def encode: Encoder[A]

  def codecRoundTrip(a: A): IsEq[Decoder.Result[A]] =
    encode(a).as(decode) <-> Right(a)

  def codecAccumulatingConsistency(json: Json): IsEq[Decoder.Result[A]] =
    decode(json.hcursor) <-> decode.decodeAccumulating(json.hcursor).leftMap(_.head).toEither
}

object CodecLaws {
  def apply[A](implicit decodeA: Decoder[A], encodeA: Encoder[A]): CodecLaws[A] = new CodecLaws[A] {
    val decode: Decoder[A] = decodeA
    val encode: Encoder[A] = encodeA
  }
}

trait CodecTests[A] extends Laws {
  def laws: CodecLaws[A]

  def codec(implicit
    arbitraryA: Arbitrary[A],
    shrinkA: Shrink[A],
    eqA: Eq[A],
    arbitraryJson: Arbitrary[Json],
    shrinkJson: Shrink[Json]
  ): RuleSet = new DefaultRuleSet(
    name = "codec",
    parent = None,
    "roundTrip" -> Prop.forAll { (a: A) =>
      laws.codecRoundTrip(a)
    },
    "consistency with accumulating" -> Prop.forAll { (json: Json) =>
      laws.codecAccumulatingConsistency(json)
    },
    "decoder serializability" -> SerializableLaws.serializable(laws.decode),
    "encoder serializability" -> SerializableLaws.serializable(laws.encode)
  )

  def unserializableCodec(implicit
    arbitraryA: Arbitrary[A],
    shrinkA: Shrink[A],
    eqA: Eq[A],
    arbitraryJson: Arbitrary[Json],
    shrinkJson: Shrink[Json]
  ): RuleSet = new DefaultRuleSet(
    name = "codec",
    parent = None,
    "roundTrip" -> Prop.forAll { (a: A) =>
      laws.codecRoundTrip(a)
    },
    "consistency with accumulating" -> Prop.forAll { (json: Json) =>
      laws.codecAccumulatingConsistency(json)
    }
  )
}

object CodecTests {
  def apply[A: Decoder: Encoder]: CodecTests[A] = new CodecTests[A] {
    val laws: CodecLaws[A] = CodecLaws[A]
  }
}
